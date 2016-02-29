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

   private String host = "www.ebi.ac.uk";
   // String query = "ebisearch/ws/rest/" + domain + "?query=" + "acc:\"" + accno + "\"%20OR%20id:\"" + accno + "\"";
   private int port = -1;

   private URL toURL(String doi) {
      try {
         URL url = new URL("http", host, port, '/' + doi.replaceAll("#", "%23").replaceAll("\\[", "%5B").replaceAll("\\]", "%5D"));
         return url;
      } catch (MalformedURLException e) {
         throw new IllegalArgumentException();
      }
   }

   /* side effect? */
   public boolean isValidID(String domain, String accno) {
     String query = "ebisearch/ws/rest/" + domain + "?query=" + "acc:\"" + accno + "\"%20OR%20id:\"" + accno + "\"";
     try {
       URL url = toURL(query);
       BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

       String inputLine;

       while ((inputLine = in.readLine()) != null) {
	 if (inputLine.contains("<hitCount>0</hitCount>")) {
           return false;
         } 	
       }
       in.close();
     } catch (IOException e) {
        ;
     }
     return true;
  }
}
