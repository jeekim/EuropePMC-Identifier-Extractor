package ukpmc

import org.specs2._

/**
   * This specification shows how to create examples using the "acceptance" style
    */
class AccResolverSpec extends Specification {
  def is = s2"""
  This is a specification to check the '10.5061/dryad.pk045' string

  The '10.5061/dryad.pk045' string should
    startWith '10.5061'                                $e1
                                                      """

  def e1 = new DoiResolver().prefixDOI("10.5061/dryad.pk045") must startWith("10.5061")
}
