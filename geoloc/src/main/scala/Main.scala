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

import geoloc.{SparkLoc, TreeConfig}
import geoloc.geometry.Point

class AkkaServer(locator: SparkLoc, host: String, port: Int) {
  implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "GeoLoc")
  implicit val executionContext: ExecutionContext = system.executionContext

  val route: Route = get {
    pathPrefix("locate" / DoubleNumber / DoubleNumber) { (lon, lat) =>
      val point = Point(lon.toFloat, lat.toFloat)

      if (locator.mapBBox.contains(point)) {
        val countriesFuture: Future[Set[String]] = Future {
          locator.locate(point)
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

  val locator = new SparkLoc(spark, Secret.pgData, TreeConfig(128, 300))

  val httpServer = new AkkaServer(locator, "localhost", 8080)

  println("Running")

  val mainIterator = Iterator.continually(StdIn.readLine())
    .takeWhile(_ != "quit")
    .map(_.split(","))
    .filter(_.size == 2)
    .map(pt => (pt(0).toFloatOption, pt(1).toFloatOption))
    .collect{ case (Some(lon), Some(lat)) => Point(lon, lat) }
    .filter(locator.mapBBox.contains)

  for (point <- mainIterator) {
    val result = locator.locate(point)

    result.size match {
      case 0 => println(s"${point} was not found")
      case 1 => println(s"${point} is in ${result.head}")
      case _ => println(s"${point} is in one of <${result.mkString(", ")}>")
    }
  }

  httpServer.stop()

  spark.close()
}
