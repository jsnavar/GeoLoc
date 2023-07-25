package geoloc.index

import geoloc.geometry.BBox

trait QSplitter[T] {
  def nw(data: T, bbox: BBox): T
  def ne(data: T, bbox: BBox): T
  def sw(data: T, bbox: BBox): T
  def se(data: T, bbox: BBox): T
}
