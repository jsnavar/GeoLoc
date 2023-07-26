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

  /* By definition, leaves of the quadtree have datasets of LabeledBox objects.
   * The splitter will assign all bounding boxes intersecting a quadrant to that
   * subtree, which means that the same bbox could end in more than one leaf.
   * While this increases the memory usage of the index, it simplifies searching,
   * as for any point p, a leaf containing p necessarily contains all bounding
   * boxes that could cover it */

  implicit val datasetSplitter: QSplitter[Dataset[LabeledBox]] = {
    /* filter bounding boxes by comparing a representative point with
     * the center of the bounding box. For example, filterBy(_.center, _ swOf _)(data, bbox)
     * will return a Dataset[LabeledBox] containing all LabeledBox objects in 'data', such
     * that their centers are to the sw of the center of 'bbox' */
    def filterBy(repr: BBox => Point, fromCenter: (Point, Point) => Boolean)(dataset: Dataset[LabeledBox], bbox: BBox) = {
      dataset.filter(lb => fromCenter(repr(lb.bbox), bbox.center))
    }

    /* By contruction, the splitter is called with a dataset and a bbox, where all
     * bounding boxes in the dataset intersect the bounding box.
     * Then, to find bboxes intersecting a quadrant, it is sufficient to verify
     * that the corresponding corner is in the same direction of the center: a box
     * intersecting 'bbox' whose sw corner is to the sw of the center of 'bbox' intersects
     * that quadrant */
    new QSplitter(filterBy(_.sw, _ swOf _),
                  filterBy(_.nw, _ nwOf _),
                  filterBy(_.ne, _ neOf _),
                  filterBy(_.se, _ seOf _))
  }
  /* branching is regulated by two parameters: max depth, and maxLeafSize */
  def branchIf(dataset: Dataset[LabeledBox], depth: Int) = {
    val maxDepth = 16
    val maxLeafSize = 1000
    dataset.count() > maxLeafSize && depth < maxDepth
  }
  val mapBBox = BBox(Point(-180f, -90f), Point(180f, 90f))

  val index: QuadTree[Dataset[LabeledBox]] = QuadTree.of(boxedDS(dbDF), mapBBox, branchIf, _.cache())

  def locate(point: Point): Set[String] = {
    val (ds, bbox) = index.get(point)
    assert(bbox.contains(point))

    ds.filter(_.bbox.contains(point))
        .map(_.label)
        .collect()
        .toSet
  }
}
