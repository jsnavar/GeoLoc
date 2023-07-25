package geoloc.index

import geoloc.geometry._

trait QuadTree[T] {
  val bbox: BBox
  def get(point: Point): (T, BBox)
}

object QuadTree {
  case class Leaf[T](payload: T, bbox: BBox) extends QuadTree[T] {
    def get(point: Point) = {
      assert(bbox.contains(point))
      (payload, bbox)
    }
  }

  case class InnerNode[T](sw: QuadTree[T],
                          nw: QuadTree[T],
                          ne: QuadTree[T],
                          se: QuadTree[T],
                          bbox: BBox) extends QuadTree[T] {
    def get(point: Point) = {
      assert(bbox.contains(point))

      val center = bbox.center
      val subTree =
        if (point.swOf(center)) sw
        else if (point.nwOf(center)) nw
        else if (point.neOf(center)) ne
        else se
      subTree.get(point)
    }
  }

  def of[T](data: T, outerBox: BBox, branch: (T, Int) => Boolean, onLeaf: T => T = (x: T) => x)(implicit sp: QSplitter[T]): QuadTree[T] = {
    def makeTree(data: T, bbox: BBox, depth: Int): QuadTree[T] = {
      if (branch(data, depth)) {
        val swBox = BBox(bbox.sw, bbox.center)
        val swData= sp.sw(data, bbox)

        val nwBox = BBox(Point(bbox.minLon, bbox.midLat), Point(bbox.midLon, bbox.maxLat))
        val nwData = sp.nw(data, bbox)

        val neBox = BBox(bbox.center, bbox.ne)
        val neData = sp.ne(data, bbox)

        val seBox = BBox(Point(bbox.midLon, bbox.minLat), Point(bbox.maxLon, bbox.midLat))
        val seData = sp.se(data, bbox)

        QuadTree.InnerNode(makeTree(swData, swBox, depth + 1),
                           makeTree(nwData, nwBox, depth + 1),
                           makeTree(neData, neBox, depth + 1),
                           makeTree(seData, seBox, depth + 1),
                           bbox)
      } else {
        QuadTree.Leaf(onLeaf(data), bbox)
      }
    }
    makeTree(data, outerBox, 0)
  }
}
