package ukpmc.scala

import monq.jfa.Xml
import java.util.Map
import scala.util.Try

case class MwtAtts(tagName: String, content: String, db: String, valMethod: String, domain: String, ctx: String, wsize: Integer, sec: String)

class MwtParser(val map: Map[String, String]) {

  // e.g., <template><z:acc db="%1" valmethod="%2" domain="%3" context="%4" wsize="%5" sec="%6">%0</z:acc></template>
  def parse() = {
    val tagName = map.get(Xml.TAGNAME)
    val content = map.get(Xml.CONTENT)

    val db = map.get("db")
    val valMethod = map.get("valmethod")
    val domain = map.get("domain")
    val ctx = map.get("context")
    val wSize = Try(map.get("wsize").toInt).toOption.getOrElse(0)
    val sec = map.getOrDefault("sec", "")

    MwtAtts(tagName, content, db, valMethod, domain, ctx, wSize, sec)
  }
}
