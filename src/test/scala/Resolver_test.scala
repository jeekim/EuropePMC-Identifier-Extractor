package ukpmc

import org.scalatest._

class ResolverSpec extends FlatSpec with Matchers {
  "doiResolver.isValidID" should "validate 10.5061/dryad.pk045 as true" in {
    val doiResolver = new DoiResolver
    doiResolver.isValid("doi", "10.5061/dryad.pk045") should be (true)
  }

  "isValid" should "validate 10.5061/dryad.pk045dd as false" in {
    new DoiResolver().isValid("doi", "10.5061/dryad.pk045dd") should be (false)
  }

  "isValid" should "validate 10.1016/dryad.pk045 as false" in {
    new DoiResolver().isValid("doi", "10.1016/dryad.pk045") should be (false)
  }

  "isValid" should "validate refseq, NC_002014 as true" in {
    new NcbiResolver().isValid("nucleotide", "NC_002014") should be (true)
  }

  "isValid" should "validate refseq, NC_0020030 as false" in {
    new NcbiResolver().isValid("nucleotide", "NC_0020030") should be (false)
  }

  "isValid" should "validate refsnp, rs6725887 as true" in {
    new NcbiResolver().isValid("snp", "rs6725887") should be (true)
  }

  "isValid" should "validate refsnp, rs67258873333 as false" in {
    new NcbiResolver().isValid("snp", "rs67258873333") should be (false)
  }

  "isValid" should "validate interpro, ipr018060 as true" in {
    new AccResolver().isValid("interpro", "ipr018060") should be (true)
  }

  "isValid" should "validate interpro, ipr01806000 as false" in {
    new AccResolver().isValid("interpro", "ipr01806000") should be (false)
  }

  "isValid" should "validate R-HSA-3108232 as true" in {
    new AccResolver().isValid("reactome", "R-HSA-3108232") should be (true)
  }

  "isValid" should "validate as GCA_000242695.1 true" in {
    new AccResolver().isValid("genome_assembly", "GCA_000242695.1") should be (true)
  }

  "isValid" should "validate as EFO:0000647 true" in {
    new AccResolver().isValid("efo", "EFO:0000647 ") should be (true)
  }

  "isCachedValid" should "validate pfam, PF00003 as true" in {
    AnnotationFilter.isIdValidInCache("pfam", "PF00003", "pfam") should be (true)
  }

  "isCachedValid" should "validate pfam, pf00003333 as false" in {
    AnnotationFilter.isIdValidInCache("pfam", "pf00003333", "pfam") should be (false)
  }

  "isOnlineValid" should "validate genome_assembly, GCA_000242695.1 as true" in {
    AnnotationFilter.isOnlineValid("gca", "GCA_000242695.1", "genome_assembly") should be (true)
  }

  "normalizeID" should "normalizes 12345.3 to 12345" in {
    new AccResolver().normalizeID("pfam", "12345.3") should be ("12345")
  }
}
