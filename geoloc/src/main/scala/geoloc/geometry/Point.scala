package geoloc.geometry

case class Point(lon: Float, lat: Float) {
  def swOf(point: Point) =
    lon <= point.lon && lat <= point.lat

  def nwOf(point: Point) =
    lon <= point.lon && lat >= point.lat

  def neOf(point: Point) =
    lon >= point.lon && lat >= point.lat

  def seOf(point: Point) =
    lon >= point.lon && lat <= point.lat
}
