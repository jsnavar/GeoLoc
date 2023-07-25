import geoloc.geometry._

class PointTest extends munit.FunSuite {
  // Simple cases, to verify that each function is in the right direction. 
  // Ordering and other properties are checked with scalacheck (in PointSpecification.scala)

  val sw = Point(-1f, -1f)
  val se = Point(1f, -1f)
  val nw = Point(-1f, 1f)
  val ne = Point(1f, 1f)

  test("swOf") {
    assert(sw.swOf(se) && sw.swOf(nw) && sw.swOf(sw))
  }

  test("nwOf") {
    assert(nw.nwOf(sw) && nw.nwOf(se) && nw.nwOf(ne))
  }

  test("neOf") {
    assert(ne.neOf(se) && ne.neOf(sw) && ne.neOf(nw))
  }

  test("seOf") {
    assert(se.seOf(nw) && se.seOf(sw) && se.seOf(ne))
  }
}
