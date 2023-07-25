import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.{Gen, Arbitrary}

import geoloc.geometry._

object BBoxSpecification extends Properties("BBox") {
  import PointSpecification.{pointGen, arbitraryPoint}
  import common._

  val bboxGen = for {
    sw <- pointGen
    ne <- pointGen
    if sw.swOf(ne)
  } yield BBox(sw, ne)

  implicit val arbitraryBBox: Arbitrary[BBox] = Arbitrary(bboxGen)

  property("contains midpoint") = forAll { (bbox: BBox) =>
    bbox.contains(Point(bbox.midLon, bbox.midLat))
  }

  property("nw") = forAll { (bbox: BBox) => {
    val nw = bbox.nw
    nw.lon == bbox.sw.lon && nw.lat == bbox.ne.lat && nw.lon == bbox.minLon && nw.lat == bbox.maxLat
  }}

  property("se") = forAll { (bbox: BBox) => {
    val se = bbox.se
    se.lon == bbox.ne.lon && se.lat == bbox.sw.lat && se.lon == bbox.maxLon && se.lat == bbox.minLat
  }}

  property("center") = forAll { (bbox: BBox) => {
    val center = bbox.center
    center.lon == bbox.midLon && center.lat == bbox.midLat
  }}

  property("contains") = forAll(bboxGen, Gen.choose(0f, 1f), Gen.choose(0f, 1f)) { 
    (bbox: BBox, x: Float, y: Float) => {
      val point = Point(bbox.sw.lon*x + bbox.ne.lon*(1-x), bbox.sw.lat*y + bbox.ne.lat*(1-y))
      bbox.contains(point)
    }
  }

  property("not contains") = forAll { (bbox: BBox, point: Point) => {
    iff(!bbox.contains(point),
        point.lon < bbox.minLon || point.lon > bbox.maxLon ||
        point.lat < bbox.minLat || point.lat > bbox.maxLat)

  }}

}
