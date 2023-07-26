package geoloc

import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.sql.SparkSession

import geoloc.geometry._
import geoloc.index._

/* postgres connection data */
case class PGData(url: String, dbtable: String, user: String, password: String)

case class LabeledBox(label: String, bbox: BBox)

class SparkLoc(spark: SparkSession, pgData: PGData) {
  import spark.implicits._
  import org.apache.spark.sql.functions._

  /* Reads the data from Postgres.
   * Schema: CREATE TABLE(label text, polygon jsonb); */
  val dbDF = spark.read
    .format("jdbc")
    .option("url", pgData.url)
    .option("dbtable", pgData.dbtable)
    .option("user", pgData.user)
    .option("password", pgData.password)
    .load()

  /* Takes a DataFrame with two columns: "label" and "polygon", computes
   * bounding boxes, and transform rows to LabeledBox objects */
  def boxedDS(table: DataFrame): Dataset[LabeledBox] = {

    /* Returns a geoloc.geometry.Point as an StructType */
    def toPoint(lon: Column, lat: Column) = map(lit("lon"), lon, lit("lat"), lat)

    /* bboxArray is (min_lon, min_lat, max_lon, max_lat).
     * Returns a struct compatible with geoloc.geometry.BBox */
    def toBBox(bboxArray: Column) = {
      val min_lon = element_at(bboxArray, 1)
      val min_lat = element_at(bboxArray, 2)
      val max_lon = element_at(bboxArray, 3)
      val max_lat = element_at(bboxArray, 4)

      map(lit("sw"), toPoint(min_lon, min_lat),
          lit("ne"), toPoint(max_lon, max_lat))
    }

    table.select($"label", from_json($"polygon", ArrayType(ArrayType(FloatType))).as("polygon"))
      .cache()
      .withColumn("bbox", aggregate($"polygon",
                                    array(lit(180f), lit(90f), lit(-180f), lit(-90f)), //(min_lon, min_lat, max_lon, max_lat)
                                    (box, point) =>
                                    array(least(element_at(box, 1), element_at(point, 1)),
                                          least(element_at(box, 2), element_at(point, 2)),
                                          greatest(element_at(box, 3), element_at(point, 1)),
                                          greatest(element_at(box, 4), element_at(point, 2)))))
      .select($"label", toBBox($"bbox").as("bbox"))
      .as[LabeledBox]
      .cache()
  }

  implicit val datasetSplitter: QSplitter[Dataset[LabeledBox]] = {
    def filterBy(repr: BBox => Point, fromCenter: (Point, Point) => Boolean)(dataset: Dataset[LabeledBox], bbox: BBox) = {
      dataset.filter(lb => fromCenter(repr(lb.bbox), bbox.center))
    }

    new QSplitter(filterBy(_.sw, _ swOf _),
                  filterBy(_.nw, _ nwOf _),
                  filterBy(_.ne, _ neOf _),
                  filterBy(_.se, _ seOf _))
  }

  def branchIf(dataset: Dataset[LabeledBox], depth: Int) = {
    val maxDepth = 16
    val maxLeafSize = 1000
    dataset.count() > maxLeafSize && depth < maxDepth
  }
  val mapBBox = BBox(Point(-180f, -90f), Point(180f, 90f))

  val index: QuadTree[Dataset[LabeledBox]] = QuadTree.of(boxedDS(dbDF), mapBBox, branchIf, _.cache())

  def locate(point: Point): Set[String] = {
    val (ds, _) = index.get(point)
    ds.filter(_.bbox.contains(point))
        .map(_.label)
        .collect()
        .toSet
  }
}
