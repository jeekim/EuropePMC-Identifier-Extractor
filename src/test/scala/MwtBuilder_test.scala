package ukpmc

import ukpmc._
import monq.jfa._
import org.scalatest._

class MwtBuilderSpec extends FlatSpec with Matchers {
  "doiResolver.isValidID" should "validate 10.5061/dryad.pk045 as true" in {
    val doiResolver = new DoiResolver
    doiResolver.isValidID("doi", "10.5061/dryad.pk045") should be (true)
  }

  "isDOIValid" should "validate 10.5061/dryad.pk045dd as false" in {
    ValidateAccessionNumber.isDOIValid("10.5061/dryad.pk045dd") should be (false)
  }

  // test for MwtBuilder.scala
}
