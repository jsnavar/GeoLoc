package geoloc.index

import geoloc.geometry.BBox

class QSplitter[T](swFn: (T, BBox) => T,
                   nwFn: (T, BBox) => T,
                   neFn: (T, BBox) => T,
                   seFn: (T, BBox) => T) {

  case class Result(sw: T, nw: T, ne: T, se: T)

  def split(data: T, bbox: BBox) = Result(swFn(data, bbox),
                                          nwFn(data, bbox),
                                          neFn(data, bbox),
                                          seFn(data, bbox))
}
