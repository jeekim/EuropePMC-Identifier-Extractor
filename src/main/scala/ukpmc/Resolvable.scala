package ukpmc.scala

trait Resolvable {
   def isValid(sem_type: String, id: String): Boolean
}
