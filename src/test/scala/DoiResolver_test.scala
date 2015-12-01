package ukpmc

import ukpmc._
import org.scalatest._

class DoiResolverSpec extends FlatSpec with Matchers {
  "DOI Resolver" should "validate the example DOI as true" in {
    val doiResolver = new DoiResolver
    doiResolver.isValidID("doi", "10.5061/dryad.pk045") should be (true)
  }

  "DOI Resolver2" should "validate the example DOI as false" in {
    ValidateAccessionNumber.isDOIValid("10.5061/dryad.pk045dd") should be (false)
  }

  "Acc online Validator" should "validate the example accession number as true" in {
    ValidateAccessionNumber.isAccValid("interpro", "ipr018060") should be (true)
  }

  "Acc online Validator2" should "validate the example accession number as false" in {
    ValidateAccessionNumber.isAccValid("interpro", "ipr01806000") should be (false)
  }

  "Cached Validator" should "validate the example accession number as true" in {
    ValidateAccessionNumber.isCachedValid("pfam", "PF00003", "pfam") should be (true)
  }

  "Cached Validator2" should "validate pf00003333 number as false" in {
    ValidateAccessionNumber.isCachedValid("pfam", "pf00003333", "pfam") should be (false)
  }

  "normalizeID" should "normalizes 12345.3 to 12345" in {
    ValidateAccessionNumber.normalizeID("pfam", "12345.3") should be ("12345")
  }

  "prefixDOI" should "prefixes 10.5061/dryad.pk045 to 10.5061" in {
    ValidateAccessionNumber.prefixDOI("10.5061/dryad.pk045") should be ("10.5061")
  }

  // TODO a test given a sentence
}
