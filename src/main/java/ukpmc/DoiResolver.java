package ukpmc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

import ukpmc.scala.Resolvable;

public class DoiResolver extends Resolver implements Resolvable {
   private final String HOST; // TODO make it more generic for any sites?
   private final int PORT;
   private static Properties prop = new Properties();
   private static Map<String, String> BlacklistDoiPrefix = new HashMap<>();

   // TODO https://api.datacite.org/works/10.5061/dryad.pk045

   public DoiResolver() {
      HOST = "data.datacite.org";
      PORT = -1;
   }

   private URL toURL(String doi) {
      try {
        String path;
        path = '/' + doi.replaceAll("#", "%23").replaceAll("\\[", "%5B") .replaceAll("\\]", "%5D");
        URL url = new URL("https", HOST, PORT, path);
        return url;
      } catch (MalformedURLException e) {
        throw new IllegalArgumentException();
      }
   }

   /**
    * return a prefix of a DOI
    */
   public String prefixDOI(String doi) {
      String prefix = "";
      int bsIndex = doi.indexOf("/");
      if (bsIndex != -1) prefix = doi.substring(0, bsIndex);
      return prefix;
   }

   public boolean isValid(String sem_type, String doi) {
      if (BlacklistDoiPrefix.containsKey(prefixDOI(doi))) {
         return false;
      } else if ("10.2210/".equals(doi.substring(0, 8))) { // exception rule for PDB data center
         return true;
      } else return isDOIValid("doi", doi);
   }

   /* public boolean isAccValid(String domain, String id) { return true; } */

   public boolean isDOIValid(String domain, String doi) {
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

   private void loadDOIPrefix() throws IOException {
      // http://stackoverflow.com/questions/27360977/how-to-read-files-from-resources-folder-in-scala
      String doiPrefixFilename = prop.getProperty("doiblacklist");
      URL pURL = DoiResolver.class.getResource("/" + doiPrefixFilename);
      BufferedReader reader = new BufferedReader(new InputStreamReader(pURL.openStream()));

      String line;
      while ((line = reader.readLine()) != null) {
         if (line.indexOf("#") != 0) {
            int firstSpace = line.indexOf(" ");
            String prefix = line.substring(0, firstSpace);
            BlacklistDoiPrefix.put(prefix, "Y");
         }
      }
      reader.close();
   }
}
