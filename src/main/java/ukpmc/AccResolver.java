package ukpmc;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class AccResolver implements IDResolver {
   private String host = "www.ebi.ac.uk";
   // private String host = "www.ebi.ac.uk/ebisearch/ws/rest";
   private int port = -1;

   private URL toURL(String doi) {
      try {
         URL url = new URL("http", host, port, 
           '/' + doi.replaceAll("#", "%23").replaceAll("\\[", "%5B").replaceAll("\\]", "%5D"));
         return url;
      } catch (MalformedURLException e) {
         throw new IllegalArgumentException();
      }
   }

   private String toString(URL url) {
      String doi = url.getPath().substring(1).replaceAll("%23", "#").replaceAll("%5B", "\\[").replaceAll("%5D", "\\]");
      return doi;
   }


   public boolean isValidID(String doi)  {
     try {
       URL url = toURL(doi);
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
