import org.apache.spark.sql.SparkSession

import geoloc.{SparkLoc, PGData, TreeConfig}
import geoloc.geometry.Point

class DbLog(pgData: PGData) {
  import java.sql.DriverManager
  import java.sql.Connection

  import scala.util.Try

  val r = new scala.util.Random
  val tag = r.nextInt()

  val connection = DriverManager.getConnection(pgData.url, pgData.user, pgData.password)

  val stmt = connection.createStatement()
  stmt.executeUpdate("create table if not exists log(run_tag integer, lon real, lat real, time double precision, result varchar, depth integer, leaf_size integer)")

  def append(point: Point, time: Double, result: String, depth: Int, leafSize: Int) = {
    val stmt = connection.createStatement()
    val query = s"insert into log values ($tag, ${point.lon}, ${point.lat}, $time, '$result', $depth, $leafSize)"
    stmt.executeUpdate(query)
  }
  def close() = {
    connection.close()
  }
}

object Bench extends App {
  val spark: SparkSession =
    SparkSession
      .builder()
      .appName("GeoLoc")
      .master("local")
      .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  val log = new DbLog(Secret.pgData)

  val points = Seq(Point(-62.164f,-22.125f),
                   Point(147.593f,-5.3157f),
                   Point(26.755f,24.0236f),
                   Point(-73.456f,-48.629f),
                   Point(126.899f,38.77f),
                   Point(14.117f,50.213f),
                   Point(-174.932f,-21.389f),
                   Point(-141.521f,-3.635f))

  val leafSizes = Seq(2000, 500)
  val depth = 65536

  for (leaf <- leafSizes) {

    val locator = new SparkLoc(spark, Secret.pgData, TreeConfig(depth, leaf))

    for (p <- points) {
      val pre = System.nanoTime
      val result = locator.locate(p)
      val post = System.nanoTime
      log.append(p, (post - pre).toDouble/1000000, result.toString.flatMap {
        case '\'' => "''"
        case c => s"$c"}, depth, leaf)
    }
  }

  log.close()
  spark.close()
}
