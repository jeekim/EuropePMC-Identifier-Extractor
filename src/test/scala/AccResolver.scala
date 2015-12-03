package ukpmc.test

import ukpmc._
import org.specs2._

/**
   * This specification shows how to create examples using the "acceptance" style
    */
class HelloWorldSpec extends Specification { def is = s2"""
  This is a specification to check the 'Hello world' string

  The 'Hello world' string should
    contain 11 characters                             $e1
    start with 'Hello'                                $e2
    end with 'world'                                  $e3
    generate '10.5061'                                $e4
                                                      """

def e1 = "Hello world" must haveSize(11)
def e2 = "Hello world" must startWith("Hello")
def e3 = "Hello world" must endWith("world")
def e4 = ValidateAccessionNumber.prefixDOI("10.5061/dryad.pk045") must startWith("10.5061")

}
