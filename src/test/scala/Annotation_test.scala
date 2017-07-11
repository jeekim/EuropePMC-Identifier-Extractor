package ukpmc

import monq.jfa._
import org.scalatest._

class AnnotationSpec extends FlatSpec with Matchers {

  "dfa_boundary" should "validate ..." in {
    val dfaRun = new DfaRun(AnnotationFilter.dfa_boundary)
    dfaRun.filter(
      """<SENT sid="34" pm="."><plain>omim <z:acc db="omim" valmethod="onlineWithContext" domain="omim" context="(?i)(o*mim)" wsize="20" sec="">603878</z:acc></plain></SENT>""") should be (
      """<SENT sid="34" pm="."><plain>omim <z:acc db="omim" ids="603878">603878</z:acc></plain></SENT>""")
    //
    dfaRun.filter(
      """<text><SENT sid="34" pm="."><plain>omim <z:acc db="omim" valmethod="onlineWithContext" domain="omim" context="(?i)(o*mim)" wsize="20" sec="">603878</z:acc></plain></SENT></text>""") should be (
      """<text><SENT sid="34" pm="."><plain>omim <z:acc db="omim" ids="603878">603878</z:acc></plain></SENT></text>""")
    //
    dfaRun.filter(
      """<article><text><SENT sid="34" pm="."><plain>omim <z:acc db="omim" valmethod="onlineWithContext" domain="omim" context="(?i)(o*mim)" wsize="20" sec="">603878</z:acc></plain></SENT></text></article>""") should be (
      """<article><text><SENT sid="34" pm="."><plain>omim <z:acc db="omim" ids="603878">603878</z:acc></plain></SENT></text></article>""")
    //
    //dfaRun.filter(
    //  """<SecTag type="TABLE"><text><SENT sid="34" pm="."><plain>omim <z:acc db="omim" valmethod="onlineWithContext" domain="omim" context="(?i)(o*mim)" wsize="20" sec="">603878</z:acc></plain></SENT><SENT sid="35" pm="."><plain><z:acc db="omim" valmethod="onlineWithContext" domain="omim" context="(?i)(o*mim)" wsize="2000" sec="XXXX">603878</z:acc></plain></SENT></text></SecTag>""") should be (
    //  """<SecTag type="TABLE"><text><SENT sid="34" pm="."><plain>omim <z:acc db="omim" ids="603878">603878</z:acc></plain></SENT><SENT sid="35" pm="."><plain><z:acc db="omim" ids="603878">603878</z:acc></plain></SENT></text></SecTag>""")
    //
    dfaRun.filter(
      """<SecTag type="FIG"><text><SENT sid="34" pm="."><plain>omim <z:acc db="omim" valmethod="onlineWithContext" domain="omim" context="(?i)(o*mim)" wsize="20" sec="">603878</z:acc></plain></SENT><SENT sid="35" pm="."><plain><z:acc db="omim" valmethod="onlineWithContext" domain="omim" context="(?i)(o*mim)" wsize="2000" sec="XXXX">603878</z:acc></plain></SENT></text></SecTag>""") should be (
      """<SecTag type="FIG"><text><SENT sid="34" pm="."><plain>omim <z:acc db="omim" ids="603878">603878</z:acc></plain></SENT><SENT sid="35" pm="."><plain>603878</plain></SENT></text></SecTag>""")
    //
    dfaRun.filter(
      """<SENT sid="0" pm="."><plain>Research was supported by Finnish Academy (141069), ERC (ERC-2009-AdG-<z:acc db="erc" valmethod="context" domain="" context="(?i)(European Research Council|ERC grant|ERC advanced grant|ERC starting grant|ERC consolidator grant|ERC proof-of-concept grant|ERC-20[0-1][0-9]-)" wsize="60" sec="XXXX">250050</z:acc>, FutureGenes) grant, Sigrid Juselius Foundation, Finnish Foundation for Cardiovascular Research (all to S.Y.); the European Research Council (ERC-2010-AdG-<z:acc db="erc" valmethod="context" domain="" context="(?i)(European Research Council|ERC grant|ERC advanced grant|ERC starting grant|ERC consolidator grant|ERC proof-of-concept grant|ERC-20[0-1][0-9]-)" wsize="60" sec="XXXX">268804</z:acc>, VESSEL network), Leducq Transatlantic Network of Excellence on Lymph Vessels in Obesity and Cardiovascular Disease (11CVD03) (all to K.A.), a VENI fellowship of the Netherlands Organization of Scientific research (to J.C.S. 016.116.017); a PhD-student fellowship from the Cardiovascular Research Institute Maastricht (to T.L.T.)  </plain></SENT>""") should be (
      """<SENT sid="0" pm="."><plain>Research was supported by Finnish Academy (141069), ERC (ERC-2009-AdG-<z:acc db="erc" ids="250050">250050</z:acc>, FutureGenes) grant, Sigrid Juselius Foundation, Finnish Foundation for Cardiovascular Research (all to S.Y.); the European Research Council (ERC-2010-AdG-<z:acc db="erc" ids="268804">268804</z:acc>, VESSEL network), Leducq Transatlantic Network of Excellence on Lymph Vessels in Obesity and Cardiovascular Disease (11CVD03) (all to K.A.), a VENI fellowship of the Netherlands Organization of Scientific research (to J.C.S. 016.116.017); a PhD-student fellowship from the Cardiovascular Research Institute Maastricht (to T.L.T.)  </plain></SENT>""")
    //
    //dfaRun.filter(
    //  """<SecTag type="ACK"><text><SENT sid="34" pm="."><plain>omim <z:acc db="omim" valmethod="onlineWithContext" domain="omim" context="(?i)(o*mim)" wsize="20" sec="ACK">603878</z:acc></plain></SENT><SENT sid="35" pm="."><plain><z:acc db="omim" valmethod="onlineWithContext" domain="omim" context="(?i)(o*mim)" wsize="2000" sec="ACK">603878</z:acc></plain></SENT></text></SecTag>""") should be (
    //  """<SecTag type="ACK"><text><SENT sid="34" pm="."><plain>omim 603878</plain></SENT><SENT sid="35" pm="."><plain>603878</plain></SENT></text></SecTag>""")
    dfaRun.filter(
      """<SENT sid="1201668" pm="."><plain>Anti-M13K07 monoclonal antibody was purchased from Amersham Biosciences (Piscataway, NJ).Table 1.Antibody specificity.AntibodyProtein specificityReovirus strainT1LT3DT3SA+5C6T1 σ1+−−<z:acc db="pdb" valmethod="onlineWithContext" domain="pdbe" context="(?i)(pdb|(?:protein +data *bank))" wsize="20000" sec="">9BG5</z:acc>T3 σ1−++8H6T1 and T3 μ1+++4F2T3D σ3−+− </plain></SENT>""") should be (
      """<SENT sid="1201668" pm="."><plain>Anti-M13K07 monoclonal antibody was purchased from Amersham Biosciences (Piscataway, NJ).Table 1.Antibody specificity.AntibodyProtein specificityReovirus strainT1LT3DT3SA+5C6T1 σ1+−−9BG5T3 σ1−++8H6T1 and T3 μ1+++4F2T3D σ3−+− </plain></SENT>"""

    )
  }
  // <z:acc db="omim" ids="603878-603890">603878 to 603890</z:acc>
}
