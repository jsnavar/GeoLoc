
object common {
  def implication(p: Boolean, q: Boolean) = !p || q
  def iff(p: Boolean, q: Boolean) = implication(p, q) && implication(q, p)
}
