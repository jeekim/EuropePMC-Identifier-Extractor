package ukpmc;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import ukpmc.scala.Resolvable;

public class AccResolver extends Resolver implements Resolvable {

   private final String HOST = "www.ebi.ac.uk";
   private final int PORT = -1;

   public boolean isValid(String domain, String accno) {
       return isAccValid(domain, accno);
   }

   private URL toURL(String doi) {
      try {
        URL url = new URL("http", HOST, PORT, '/' + doi.replaceAll("#", "%23")
                .replaceAll("\\[", "%5B").replaceAll("\\]", "%5D"));
        return url;
      } catch (MalformedURLException e) {
        throw new IllegalArgumentException();
      }
   }

   private boolean isAccValid(String domain, String accno) {
     if ("efo".equals(domain)) {
        accno = extractNumbers(accno);
         // System.err.println(accno);
     } else if ("reactome".equals(domain)) {
         accno = extractNumbers(accno);
     }
     // String query = "ebisearch/ws/rest/" + domain + "?query=" + accno;
     String query = "ebisearch/ws/rest/" + domain + "?query=" + "acc:\"" + accno + "\"%20OR%20id:\"" + accno + "\"";
     URL url = toURL(query);
     try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
       String line;
       while ((line = in.readLine()) != null) {
	     if (line.contains("<hitCount>0</hitCount>")) return false;
       }
       in.close();
     } catch (IOException e) {
         System.err.println(e);
     }
     return true;
   }

   private String extractNumbers(String accno) {
       Pattern p = Pattern.compile("\\d+");
       Matcher m = p.matcher(accno);
       if (m.find()) {
           return m.group();
       }
       return accno;
   }
}
