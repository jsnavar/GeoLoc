package geoloc.geometry

object functions {
  def rectify(polygon: Seq[Point]): Seq[Point] = {
    if (polygon.head == polygon.last)  polygon
    else polygon :+ polygon.head
  }

  /** Finds \alpha such that
   * (\alpha \cdot 'from') + ((1 - \alpha)\cdot 'to')
   * has the same latitude as 'point' */
  def interpolateOnY(point: Point, from: Point, to: Point): Float = {
    // if point.lat == to.lat == from.lat returns NaN.
    // if point.lat != to.lat but from.lat == to.lat return Infinity or -Infinity.
    // Otherwise returns \alpha as a normal float
    (point.lat - to.lat) / (from.lat - to.lat)
  }

  /** count crossings of the ray from the point to the right */
  def insideOf(point: Point, polygon: Seq[Point]): Boolean = {
    // appends the second element of the polygon, and iterate triples.
    // In this way, every segment of the polygon appears
    // once at the beginning of the triple, followed by the next vertex
    val tripleIterator = rectify(polygon).appended(polygon(1)).sliding(3)

    val crossings = tripleIterator
      .map { arr => (interpolateOnY(point, arr(0), arr(1)), arr) }
      .filter{ case (alpha, _) => alpha >= 0.0f && alpha < 1.0f }
      .count { case (alpha, arr) => alpha match {
        case 0f => {
          /* same y as arr(1) */
          point == arr(1) || {
            /* disc < 0 iff arr(0) and arr(2) are to the same side of point,
             * meaning that arr(1) is a maximum or minimum point */
            val disc = (arr(2).lat - point.lat) * (point.lat - arr(0).lat)
            disc >= 0 && arr(1).lon >= point.lon
          }
        }
        case a => {
          val intersectionLon = arr(0).lon * alpha + arr(1).lon * (1 - alpha)
          intersectionLon >= point.lon
        }
      }}

    crossings % 2 == 1;
  }
}
