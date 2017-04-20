package ukpmc;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import ukpmc.scala.IDResolver;

public class DoiResolver implements IDResolver {
   private final String HOST; // TODO make it more generic for any sites?
   private int port;

   public DoiResolver() {
      HOST = "data.datacite.org";
      port = -1;
   }

   private URL toURL(String doi) {
      try {
        String path = '/' + doi.replaceAll("#", "%23").replaceAll("\\[", "%5B").replaceAll("\\]", "%5D");
        URL url = new URL("https", HOST, port, path);
        return url;
      } catch (MalformedURLException e) {
        throw new IllegalArgumentException();
      }
   }

   public boolean isValidID(String domain, String doi) {
      try {
         URL url = toURL(doi);
         HttpURLConnection connection = (HttpURLConnection) url.openConnection();
         String response = connection.getResponseMessage();
         connection.disconnect();
         return response.equals("OK");
      } catch (MalformedURLException e) {
         throw new IllegalArgumentException();
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }
}
