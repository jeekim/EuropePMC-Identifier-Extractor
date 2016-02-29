package ukpmc.test

import org.specs2._
import matcher.DataTables
// import specification.script.GWT

/**
* This specification shows how to use a simple Datatable
*/
class DataTablesSpec extends Specification with DataTables { def is = s2"""

  Adding integers should just work in scala $addition"""

  // TODO to replace with accession numbers (true, false)

  def addition =

    "a"   | "b" | "c" |>
     2    !  2  !  4  |
     1    !  1  !  2  | { (a, b, c) =>  a + b === c }
}
