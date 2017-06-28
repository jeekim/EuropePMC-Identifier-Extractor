package ukpmc

// import monq.jfa._
import org.scalatest._

class ResolverSpec extends FlatSpec with Matchers {
  /* "doiResolver.isValidID" should "validate 10.5061/dryad.pk045 as true" in {
    val doiResolver = new DoiResolver
    doiResolver.isValidID("doi", "10.5061/dryad.pk045") should be (true)
  } */

  "isDOIValid" should "validate 10.5061/dryad.pk045dd as false" in {
    new DoiResolver().isValid("doi", "10.5061/dryad.pk045dd") should be (false)
  }

  // TODO doi blacklist test

  "isAccValid" should "validate interpro, ipr018060 as true" in {
    new AccResolver().isAccValid("interpro", "ipr018060") should be (true)
    // AccessionNumberFilter.isAccValid("interpro", "ipr018060") should be (true)
  }

  "isAccValid" should "validate interpro, ipr01806000 as false" in {
    new AccResolver().isAccValid("interpro", "ipr01806000") should be (false)
    // AccessionNumberFilter.isAccValid("interpro", "ipr01806000") should be (false)
  }

  "isCachedValid" should "validate pfam, PF00003 as true" in {
    AnnotationFilter.isIdValidInCache("pfam", "PF00003", "pfam") should be (true)
  }

  "isCachedValid" should "validate pfam, pf00003333 as false" in {
    AnnotationFilter.isIdValidInCache("pfam", "pf00003333", "pfam") should be (false)
  }

  "normalizeID" should "normalizes 12345.3 to 12345" in {
    new AccResolver().normalizeID("pfam", "12345.3") should be ("12345")
    // AccessionNumberFilter.normalizeID("pfam", "12345.3") should be ("12345")
  }

  /* "prefixDOI" should "prefixes 10.5061/dryad.pk045 to 10.5061" in {
    AccessionNumberFilter.prefixDOI("10.5061/dryad.pk045") should be ("10.5061")
  } */
  // <z:acc db="omim" ids="603878-603890">603878 to 603890</z:acc>
}
