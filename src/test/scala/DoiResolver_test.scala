package ukpmc

import ukpmc._
import monq.jfa._
import org.scalatest._

class DoiResolverSpec extends FlatSpec with Matchers {
  "doiResolver.isValidID" should "validate 10.5061/dryad.pk045 as true" in {
    val doiResolver = new DoiResolver
    doiResolver.isValidID("doi", "10.5061/dryad.pk045") should be (true)
  }

  "isDOIValid" should "validate 10.5061/dryad.pk045dd as false" in {
    ValidateAccessionNumber.isDOIValid("10.5061/dryad.pk045dd") should be (false)
  }

  "isAccValid" should "validate interpro, ipr018060 as true" in {
    ValidateAccessionNumber.isAccValid("interpro", "ipr018060") should be (true)
  }

  "isAccValid" should "validate interpro, ipr01806000 as false" in {
    ValidateAccessionNumber.isAccValid("interpro", "ipr01806000") should be (false)
  }

  "isCachedValid" should "validate pfam, PF00003 as true" in {
    ValidateAccessionNumber.isCachedValid("pfam", "PF00003", "pfam") should be (true)
  }

  "isCachedValid" should "validate pfam, pf00003333 as false" in {
    ValidateAccessionNumber.isCachedValid("pfam", "pf00003333", "pfam") should be (false)
  }

  "normalizeID" should "normalizes 12345.3 to 12345" in {
    ValidateAccessionNumber.normalizeID("pfam", "12345.3") should be ("12345")
  }

  "prefixDOI" should "prefixes 10.5061/dryad.pk045 to 10.5061" in {
    ValidateAccessionNumber.prefixDOI("10.5061/dryad.pk045") should be ("10.5061")
  }

  "dfa_boundary" should "validate ..." in {
    val dfaRun = new DfaRun(ValidateAccessionNumber.dfa_boundary)
    dfaRun.filter("""<SENT sid="34" pm="."><plain>omim <z:acc db="omim" valmethod="onlineWithContext" domain="omim" context="(?i)(o*mim)" wsize="20">603878</z:acc></plain></SENT>""") should be ("""<SENT sid="34" pm="."><plain>omim <z:acc db="omim" ids="603878">603878</z:acc></plain></SENT>""")
    dfaRun.filter("""<text><SENT sid="34" pm="."><plain>omim <z:acc db="omim" valmethod="onlineWithContext" domain="omim" context="(?i)(o*mim)" wsize="20">603878</z:acc></plain></SENT></text>""") should be ("""<text><SENT sid="34" pm="."><plain>omim <z:acc db="omim" ids="603878">603878</z:acc></plain></SENT></text>""")
    dfaRun.filter("""<article><text><SENT sid="34" pm="."><plain>omim <z:acc db="omim" valmethod="onlineWithContext" domain="omim" context="(?i)(o*mim)" wsize="20">603878</z:acc></plain></SENT></text></article>""") should be ("""<article><text><SENT sid="34" pm="."><plain>omim <z:acc db="omim" ids="603878">603878</z:acc></plain></SENT></text></article>""")
    dfaRun.filter("""<table><text><SENT sid="34" pm="."><plain>omim <z:acc db="omim" valmethod="onlineWithContext" domain="omim" context="(?i)(o*mim)" wsize="20">603878</z:acc></plain></SENT><SENT sid="35" pm="."><plain><z:acc db="omim" valmethod="onlineWithContext" domain="omim" context="(?i)(o*mim)" wsize="2000">603878</z:acc></plain></SENT></text></table>""") should be ("""<table><text><SENT sid="34" pm="."><plain>omim <z:acc db="omim" ids="603878">603878</z:acc></plain></SENT><SENT sid="35" pm="."><plain><z:acc db="omim" ids="603878">603878</z:acc></plain></SENT></text></table>""")
  }

  // <z:acc db="omim" ids="603878-603890">603878 to 603890</z:acc>
}
