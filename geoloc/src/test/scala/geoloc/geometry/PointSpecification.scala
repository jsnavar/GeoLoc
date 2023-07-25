import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.{Gen, Arbitrary}

import geoloc.geometry.Point

object PointSpecification extends Properties("Point") {
  val pointGen = for {
    lon <- Gen.choose(-180f, 180f)
    lat <- Gen.choose(-90f, 90f)
  } yield Point(lon, lat)

  def implication(p: Boolean, q: Boolean) = !p || q

  implicit val arbitraryPoint: Arbitrary[Point] = Arbitrary(pointGen)

  property("reflexive") = forAll { (p: Point) =>
    p.swOf(p) && p.nwOf(p) && p.neOf(p) && p.seOf(p)
  }

  property("antisymmetric") = forAll { (p1: Point, p2: Point) => {
    implication(p1.swOf(p2) && p2.swOf(p1), p1 == p2) &&
      implication(p1.seOf(p2) && p2.seOf(p1), p1 == p2) &&
      implication(p1.nwOf(p2) && p2.nwOf(p1), p1 == p2) &&
      implication(p1.neOf(p2) && p2.neOf(p1), p1 == p2)
  }}

  property("transitive") = forAll { (p1: Point, p2: Point) => {
    val middle = Point((p1.lon + p2.lon) / 2, (p1.lat + p2.lat) / 2)

    implication(p1.seOf(middle) && middle.seOf(p2), p1.seOf(p2)) &&
      implication(p1.swOf(middle) && middle.swOf(p2), p1.swOf(p2)) &&
      implication(p1.neOf(middle) && middle.neOf(p2), p1.neOf(p2)) &&
      implication(p1.nwOf(middle) && middle.nwOf(p2), p1.nwOf(p2))
  }}

  property("longitude") = forAll { (p1: Point, p2: Point) => {
    // if p1 is to the east and west of p2, then they have the same longitude
    implication((p1.seOf(p2) || p1.neOf(p2)) && (p1.swOf(p2) || p1.nwOf(p2)), p1.lon == p2.lon)
  }}

  property("latitude") = forAll { (p1: Point, p2: Point) => {
    implication((p1.seOf(p2) || p1.swOf(p2)) && (p1.neOf(p2) || p1.nwOf(p2)), p1.lat == p2.lat)
  }}
}
