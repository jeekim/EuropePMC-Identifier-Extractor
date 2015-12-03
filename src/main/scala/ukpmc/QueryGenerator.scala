// package ukpmc
package ukpmc.scala

// TODO use pattern matching to generate queries depending on db types.

case class QueryGenerator(q: String) {

  override def toString() = s"query: $q"

}

object Dictionary {

  def hallo() { println("Hallo (object)") }
  val MAX_SIZE = 100

}
