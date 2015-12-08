package ukpmc.scala

trait IDResolver {
   def isValidID(domain: String, id: String): Boolean 
}
