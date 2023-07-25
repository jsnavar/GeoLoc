package geoloc.geometry


case class BBox(sw: Point, ne: Point) {
  require(sw.swOf(ne))

  def minLon = sw.lon
  def maxLon = ne.lon
  def midLon = (sw.lon + ne.lon)/2

  def minLat = sw.lat
  def maxLat = ne.lat
  def midLat = (sw.lat + ne.lat)/2

  val se = Point(maxLon, minLat)
  val nw = Point(minLon, maxLat)
  val center = Point(midLon, midLat)

  def contains(point: Point): Boolean = {
    point.lat >= minLat &&
      point.lat <= maxLat &&
      point.lon >= minLon &&
      point.lon <= maxLon
  }
}
