package ukpmc;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import ukpmc.scala.IDResolver;

public class AccResolver implements IDResolver {

   // String query = "ebisearch/ws/rest/" + domain + "?query=" + "acc:\"" + accno + "\"%20OR%20id:\"" + accno + "\"";
   private final String HOST = "www.ebi.ac.uk";
   private final int PORT = -1;

   private URL toURL(String doi) {
      try {
        URL url = new URL("http", HOST, PORT, '/' + doi.replaceAll("#", "%23")
                .replaceAll("\\[", "%5B").replaceAll("\\]", "%5D"));
        return url;
      } catch (MalformedURLException e) {
        throw new IllegalArgumentException();
      }
   }

   /* side effect? */
   public boolean isValidID(String domain, String accno) {
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
}
