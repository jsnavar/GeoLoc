import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.{Gen, Arbitrary}

import geoloc.geometry._
import geoloc.index._

object QuadTreeSpecification extends Properties("QuadTree") {
  import common._
  import PointSpecification.{pointGen, arbitraryPoint}

  type PointSetTree = QuadTree[Set[Point]]
  val mapBBox = BBox(Point(-180f, -90f), Point(180f, 90f))

  implicit val pointSetSplitter: QSplitter[Set[Point]] = new QSplitter[Set[Point]] {
    def nw(points: Set[Point], bbox: BBox) = points.filter(p => p.nwOf(bbox.center))
    def ne(points: Set[Point], bbox: BBox) = points.filter(p => p.neOf(bbox.center))
    def sw(points: Set[Point], bbox: BBox) = points.filter(p => p.swOf(bbox.center))
    def se(points: Set[Point], bbox: BBox) = points.filter(p => p.seOf(bbox.center))
  }

  property("right leaf") = forAll { (points: Set[Point]) => {
    val tree = QuadTree.of(points, mapBBox, (set: Set[Point],_) => set.size > 2)
    points.forall(p => {
      val (s, box) = tree.get(p)
      s.contains(p) && box.contains(p)
    })
  }}
}
