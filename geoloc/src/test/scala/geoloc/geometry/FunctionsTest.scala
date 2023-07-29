import geoloc.geometry._

class FunctionsTest extends munit.FunSuite {
  test("insideOf 1") {
    import functions.insideOf

    val xTriangle = Seq(Point(-1f, -1f), Point(0f, 1f), Point(1f, -1f), Point(-1f, -1f))

    assert(insideOf(Point(0f, 0f), xTriangle))
    assert(insideOf(Point(-1f, -1f), xTriangle))
    assert(insideOf(Point(-0.4f, -1f), xTriangle))
    assert(!insideOf(Point(-1f, 0f), xTriangle))
    assert(!insideOf(Point(-1f, 1f), xTriangle))
  }

  test("insideOf 2") {
    import functions.insideOf

    val yTriangle = Seq(Point(-1f, -1f), Point(-1f, 1f), Point(1f, 0f), Point(-1f, -1f))

    assert(insideOf(Point(0f, 0f), yTriangle))
    assert(insideOf(Point(-1f, -1f), yTriangle))
    assert(insideOf(Point(-0.999f, 0.2f), yTriangle))
    assert(!insideOf(Point(-1f, 0f), yTriangle))
  }
}
