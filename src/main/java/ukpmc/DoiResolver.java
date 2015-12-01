package ukpmc;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import ukpmc.QueryGenerator;
// import ukpmc.scala.QueryGenerator;

public class DoiResolver implements IDResolver {
   private String host = "data.datacite.org"; // TODO make it more generic for any sites?
   private int port = -1;

   private URL toURL(String doi) {
      try {
        URL url = new URL("http", host, port, '/' + doi.replaceAll("#", "%23").replaceAll("\\[", "%5B").replaceAll("\\]", "%5D"));
        return url;
      } catch (MalformedURLException e) {
        throw new IllegalArgumentException();
      }
   }

   /* private String toString(URL url) {
      String doi = url.getPath().substring(1).replaceAll("%23", "#").replaceAll("%5B", "\\[").replaceAll("%5D", "\\]");
      return doi;
   } */

   public boolean isValidID(String domain, String doi) {

      // QueryGenerator qg = new QueryGenerator("10101");
      // System.err.println(qg);

      try {
         URL url = toURL(doi);
         HttpURLConnection connection = (HttpURLConnection) url.openConnection();
         String response = connection.getResponseMessage();
         connection.disconnect();
         if (response.equals("OK")) {
            return true;
         } else {
            return false;
         }
      } catch (MalformedURLException e) {
         throw new RuntimeException(e);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }
}
