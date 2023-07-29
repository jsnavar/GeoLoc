import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import org.apache.spark.sql.SparkSession

import geoloc.SparkLoc
import geoloc.geometry.Point

class AkkaServer(locator: SparkLoc, host: String, port: Int) {
  implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "GeoLoc")
  implicit val executionContext: ExecutionContext = system.executionContext

  val route: Route = get {
    pathPrefix("locate" / DoubleNumber / DoubleNumber) { (lon, lat) =>
      val point = Point(lon, lat)
      if (locator.mapBBox.contains(point)) {
        val countriesFuture: Future[Set[String]] = Future {
          locator.locate(Point(lon.toFloat, lat.toFloat))
        }

        onSuccess(countriesFuture) { countries =>
          if(countries.isEmpty) complete(StatusCodes.NotFound)
          else complete(countries)
        }
      } else {
        complete(StatusCodes.BadRequest)
      }
    }
  }

  val bindingFuture = Http().newServerAt(host, port).bind(route)

  def stop() = bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
}

object Main extends App {
  val spark: SparkSession =
    SparkSession
      .builder()
      .appName("GeoLoc")
      .master("local")
      .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  val locator = new SparkLoc(spark, Secret.pgData)

  val httpServer = new AkkaServer(locator, "localhost", 8080)

  println("running")

  val mainIterator = Iterator.continually(StdIn.readLine())
    .takeWhile(_ != "quit")
    .map(_.split(","))
    .filter(_.size == 2)
    .map(sq => (sq(0).toFloatOption, sq(1).toFloat))
    .collect{ case (Some(lon), Some(lat)) => Point(lon, lat) }
    .filter(locator.mapBBox.contains)

  for (point <- mainIterator) {
    println(locator.locate(point))
  }

  httpServer.stop()

  spark.close()
}
