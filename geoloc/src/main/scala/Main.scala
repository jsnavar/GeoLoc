import scala.io.StdIn

import org.apache.spark.sql.SparkSession

import geoloc.SparkLoc

object Main extends App {
  import geoloc.geometry.Point

  val spark: SparkSession =
    SparkSession
      .builder()
      .appName("GeoLoc")
      .master("local")
      .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  val locator = new SparkLoc(spark, Secret.pgData)

  locator.locate(Point(0f, 0f))
  println("running")

  val line = StdIn.readLine().split(" ")
  val mainIterator = Iterator.continually(StdIn.readLine())
    .takeWhile(_ != "quit")
    .map(_.split(","))
    .filter(_.size == 2)
    .map(sq => Point(sq(0).toFloat, sq(1).toFloat))

  for (point <- mainIterator) {
    println(locator.locate(point))
  }
  spark.close()
}
