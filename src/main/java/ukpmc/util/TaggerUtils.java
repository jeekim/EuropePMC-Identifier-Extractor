package ukpmc.util;

import monq.jfa.Xml;
import java.util.Map;

/**
 * Created by jee on 27/06/17.
 */
public class TaggerUtils {
    /**
     *
     */
    public static String reEmbedContent(String taggedF, StringBuilder yytext, Map<String, String> map, int start) {
        int contentBegins = yytext.indexOf(map.get(Xml.CONTENT), start);
        int contentLength = map.get(Xml.CONTENT).length();
        return yytext.substring(start, contentBegins) + taggedF
                + yytext.substring(contentBegins + contentLength);
    }
}
