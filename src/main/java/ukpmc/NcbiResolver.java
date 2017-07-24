package ukpmc;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import ukpmc.scala.Resolvable;

public class NcbiResolver extends Resolver implements Resolvable {

   private final String HOST = "eutils.ncbi.nlm.nih.gov";
   private final int PORT = -1;

   public boolean isValid(String domain, String accno) {
       return isAccValid(domain, accno);
   }

   private URL toURL(String doi) {
      try {
        URL url = new URL("https", HOST, PORT, '/' + doi.replaceAll("#", "%23")
                .replaceAll("\\[", "%5B").replaceAll("\\]", "%5D"));
        return url;
      } catch (MalformedURLException e) {
        throw new IllegalArgumentException();
      }
   }

   private boolean isAccValid(String domain, String accno) {
     String query = "entrez/eutils/esearch.fcgi?db=" + domain + "&term=" + accno; //  + "\"%20OR%20id:\"" + accno + "\"";
     URL url = toURL(query);
     try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
       String line;
       while ((line = in.readLine()) != null) {
         // TODO make the check more robust
	     if (line.contains("<Count>0</Count>")) return false;
       }
       in.close();
     } catch (IOException e) {
         System.err.println(e);
     }
     return true;
   }
}
