package ukpmc

import ukpmc._
import org.scalatest._

class DoiResolverSpec extends FlatSpec with Matchers {

  "DOI Resolver" should "validate the example DOI as true" in {
    val doiResolver = new DoiResolver
    doiResolver.isValidID("10.5061/dryad.pk045") should be (true)
    // DoiResolver.isValidID("10.5061/dryad.pk045") should be (true)
  }

  "EB-eye Validator" should "validate the example accession number as true" in {
    // val doiResolver = new DoiResolver
    ValidateAccessionNumber.ebEyeValidate("interpro", "ipr018060")
  }
}
