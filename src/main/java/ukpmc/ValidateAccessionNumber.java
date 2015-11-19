package ukpmc;

/**
 * Validate Accession Number spotting
 * 
 * Authors: Ian Lewin and Jee-Hyub Kim
 *
 * Looks for <z:acc> elements and attempts to validate the accession number
 * 
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

import java.net.ServerSocket;
import java.net.URL;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import monq.jfa.AbstractFaAction;
import monq.jfa.Dfa;
import monq.jfa.DfaRun;
import monq.jfa.Nfa;
import monq.jfa.Xml;

import monq.net.FilterServiceFactory;
import monq.net.Service;
import monq.net.ServiceCreateException;
import monq.net.ServiceFactory;
import monq.net.TcpServer;

// TODO to move to another class only for EB-eye client test.
import uk.ac.ebi.webservices.jaxws.stubs.ebeye.*;
import uk.ac.ebi.webservices.jaxws.EBeyeClient;


@SuppressWarnings("serial")
public class ValidateAccessionNumber implements Service {

   private static final Logger LOGGER = Logger.getLogger(ValidateAccessionNumber.class.getName()); 

   private static TcpServer svr = null;

   private static EBeyeClient ebeye = new EBeyeClient();
   private static DoiResolver dr = new DoiResolver();
   private static AccResolver ar = new AccResolver();
   private static Properties prop = new Properties();
   
   private static Dfa dfa_access = null;
   private static Dfa dfa_boundary = null;
   
   private static Map<String, String> cachedValidations = new HashMap<String, String>();
   private static Map<String, String> cachedDoiPrefix = new HashMap<String, String>();
   private static Map<String, Integer> numOfAccInBoundary = new HashMap<String, Integer>();
 
   private InputStream in = null;
   private OutputStream out = null;

   public ValidateAccessionNumber(InputStream in, OutputStream out) {
      this.in = in;
      this.out = out;
   }

   /**
    * @throws IOException
    * checks that validate.properties exists and load it
    */
   private static void loadConfigurationFile() throws IOException {
      URL url = ValidateAccessionNumber.class.getResource("/validate.properties");
      if (url == null) {
         throw new RuntimeException("can not find validate.properties!");
      }
      prop.load(url.openStream());
   }

   /**
    * Read the stored list of predefined results and fill a cache
    * 
    * Note that nothing enforces that the file defines a MAP (there could
    * be more than entry for the same KEY).But this code will just
    * overwrite earlier entries with later ones if this happens.
    * @throws IOException
    */
   private static void loadPredefinedResults() throws IOException {
      String predeffilename = prop.getProperty("cached");
      URL pURL = ValidateAccessionNumber.class.getResource("/" + predeffilename);

      BufferedReader reader = new BufferedReader(new InputStreamReader(pURL.openStream()));
      String line = reader.readLine();

      while (line != null) {
         if (line.indexOf("#") != 0) {
            int firstspace = line.indexOf(" ");
            int secspace = line.indexOf(" ", firstspace + 1);
            String accno = line.substring(0, firstspace);
            String db = line.substring(firstspace + 1, secspace);
            cachedValidations.put(db + accno, line);
         }
         line = reader.readLine();
      }
      reader.close();
   }
   
   /**
    * Read the stored list of DOI prefixes for articles only
    */
   private static void loadDOIPrefix() throws IOException {
      String doiprefixfilename = prop.getProperty("doiblacklist");
      // URL pURL = ValidateAccessionNumber.class.getResource("/doi.prefix.1000.tsv");
      // http://stackoverflow.com/questions/27360977/how-to-read-files-from-resources-folder-in-scala
      URL pURL = ValidateAccessionNumber.class.getResource("/" + doiprefixfilename);
      BufferedReader reader = new BufferedReader(new InputStreamReader(pURL.openStream()));
      String line = reader.readLine();

      while (line != null) {
         if (line.indexOf("#") != 0) {
            int firstspace = line.indexOf(" ");
            String prefix = line.substring(0, firstspace);
            cachedDoiPrefix.put(prefix, "Y");
         }
         line = reader.readLine();
      }
      reader.close();
   }

   /**
    *
    */
   private static String reEmbedContent(String taggedF, StringBuffer yytext, Map<String, String> map, int start) {
      int contentBegins = yytext.indexOf(map.get(Xml.CONTENT), start);
      int contentLength = map.get(Xml.CONTENT).length();
      StringBuilder newelem = new StringBuilder();
      newelem.append(yytext.substring(start, contentBegins));
      newelem.append(taggedF);
      newelem.append(yytext.substring(contentBegins + contentLength));
      return newelem.toString();
   }

   /**
    *
    */
   private static AbstractFaAction procBoundary = new AbstractFaAction() {
      public void invoke(StringBuffer yytext, int start, DfaRun runner) {
         numOfAccInBoundary = new HashMap<String, Integer>();
         try {
            Map <String, String> map = Xml.splitElement(yytext, start);
            String xmlcontent = map.get(Xml.CONTENT);
            DfaRun procplain = new DfaRun(dfa_access);
            String newoutput = procplain.filter(xmlcontent);
            String embedcontent = reEmbedContent(newoutput, yytext, map, start);
            yytext.replace(start, yytext.length(), embedcontent);
         } catch (Exception e) {
            LOGGER.log(Level.INFO, "context", e);
         }
      }
   };


   /**
    *  This processes an accession number
    *  noval: refseq, refsnp, context: eudract offline: pfam, online (+ offline): the rest
    */   
   private static AbstractFaAction procAccession = new AbstractFaAction() {
      public void invoke(StringBuffer yytext, int start, DfaRun runner) {
         LOGGER.setLevel(Level.SEVERE);
         try { 
            Map<String, String> map = Xml.splitElement(yytext, start);

            String xmlcontent = map.get(Xml.CONTENT);
            String db = map.get("db");
            String valmethod = map.get("valmethod");
            String domain = map.get("domain");
            String context = map.get("context");
            String wsize = map.get("wsize");

            LOGGER.info(db + ": " + ":" + xmlcontent + ":" + valmethod + ": " + domain + ": " + context + ":" + start + ":");
            String tagged = "<z:acc db=\"" + db + "\" ids=\"" + xmlcontent +"\">" + xmlcontent + "</z:acc>";

            if ("noval".equals(valmethod)) {
               LOGGER.info(xmlcontent + ": in the noval.");
               numOfAccInBoundary.put(db, 1);
               yytext.replace(start, yytext.length(), tagged);
               return;
            }


            if (valmethod.matches("(.*)contextOnly(.*)")) {
               if (isAnySameTypeBefore(db) || isInContext(yytext, start, context, wsize)) {
                  LOGGER.info(xmlcontent + ": in the context.");
                  numOfAccInBoundary.put(db, 1);
                  yytext.replace(start, yytext.length(), tagged);
                  return;
               }
            }

            if (valmethod.matches("(.*)WithContext(.*)")) {
               if (valmethod.matches("(.*)cached(.*)")) {
                  if (isCachedValid(db, xmlcontent, domain) && (isAnySameTypeBefore(db) || isInContext(yytext, start, context, wsize))) {
                     LOGGER.info(xmlcontent + ": in the cache with context.");
                     numOfAccInBoundary.put(db, 1);
                     yytext.replace(start, yytext.length(), tagged);
                     return;
                  }
               }
               if (valmethod.matches("(.*)online(.*)")) {
                  if ((isAnySameTypeBefore(db) || isInContext(yytext, start, context, wsize)) && isOnlineValid(db, xmlcontent, domain)) {
                     LOGGER.info(xmlcontent + ": in the online with context.");
                     numOfAccInBoundary.put(db, 1);
                     yytext.replace(start, yytext.length(), tagged);
                     return;
                  }
               }
            } else {
               if (valmethod.matches("(.*)cached(.*)")) {
                  if (isCachedValid(db, xmlcontent, domain)) {
                     LOGGER.info(xmlcontent + ": in the cache.");
                     numOfAccInBoundary.put(db, 1);
                     yytext.replace(start, yytext.length(), tagged);
                     return;
                  }
               }
               if (valmethod.matches("(.*)online(.*)")) {
                  if (isOnlineValid(db, xmlcontent, domain)) {
                     LOGGER.info(xmlcontent + ": in the online.");
                     numOfAccInBoundary.put(db, 1);
                     yytext.replace(start, yytext.length(), tagged);
                     return;
                  }
               }
            }
            yytext.replace(start, yytext.length(), xmlcontent);

         } catch (Exception e) {
            LOGGER.log(Level.INFO, "context", e);
         }
      }
   };

   /**
    *
    */
   private static boolean isAnySameTypeBefore(String db) {
      if ("erc".equals(db)) { return false; }
      return numOfAccInBoundary.containsKey(db);
   }
   
   /**
    *
    */
   private static boolean isInContext(StringBuffer yytext, int start, String context, String wsize) {
      Integer wSize = Integer.parseInt(wsize);
      Integer pStart = start - wSize;

      if (pStart < 0) {
         pStart = 0;
      }

      Pattern p = Pattern.compile(context);
      Matcher m = p.matcher(yytext.substring(pStart, start));

      return m.find();
   }

   /**
    * normalize accession numbers for cached and online validation
    */
   private static String normalizeAcc(String db, String accno) {
      int dotIndex = accno.indexOf("."); // if it's a dotted Accession number, then only test the prefix

      if (dotIndex != -1 && !"doi".equals(db)) {
         accno = accno.substring(0, dotIndex);
      }
      if (")".equals(accno.substring(accno.length() - 1))) {
         accno = accno.substring(0, accno.length() - 1);
      }
      return accno.toUpperCase();
   }

   /**
    * return a prefix of a DOI
    */
   private static String prefixDOI(String doi) {
      String prefix = new String();
      int bsIndex = doi.indexOf("/");

      if (bsIndex != -1) {
         prefix = doi.substring(0, bsIndex);
      }           
      return prefix;
   }

   /**
    *
    */
   private static boolean isCachedValid(String db, String accno, String domain) {
      String validationResult = new String();
      accno = normalizeAcc(db, accno);

      if (cachedValidations.containsKey(domain + accno)) {
         validationResult = cachedValidations.get(domain + accno);
         return isResultValid(validationResult, accno, domain);
      } else {
        return false;
      }
   }

   /**
    *
    */
   private static boolean isDOIValid(String accno) { // TODO move to DoiResolver?
      if (cachedDoiPrefix.containsKey(prefixDOI(accno))) {
         LOGGER.info(accno + ": in the black list.");
         return false;
      }
      if ("10.2210/".equals(accno.substring(0, 8))) { // exception rule for PDB data center
         LOGGER.info(accno + ": in PDB data center.");
         return true;
      } else if (dr.isValidID(accno)) {
         LOGGER.info(accno + ": is a valid id.");
         return true;
      } else {
         LOGGER.info(accno + ": is not a valid id.");
         return false;
      }
   }

   public static boolean isAccValid(String domain, String accno) {
      // acc:"IPR018060"%20OR%20id:"IPR018060" 
      // String query = "ebisearch/ws/rest/" + domain + "?query=" + "id:\"" + accno + "\"";
      String query = "ebisearch/ws/rest/" + domain + "?query=" + "acc:\"" + accno + "\"%20OR%20id:\"" + accno + "\"";
      if (ar.isValidID(query)) {
        return true;
      } else {
        return false;
      }

   }

   /**
    * pdb and uniprot is case-insensitive, but ENA is upper-case
    */
   private static boolean isOnlineValid(String db, String accno, String domain) {
      String validationResult = new String();
      accno = normalizeAcc(db, accno);

      if ("doi".equals(db)) { // special case
         return isDOIValid(accno);
      } else { // EB-eye validation
         return isAccValid(domain, accno);
         /* try {
            validationResult = ebEyeValidate(domain, accno);
            return isResultValid(validationResult, accno, domain);
         } catch (Exception e) {
            LOGGER.log(Level.INFO, "context", e);
         } */
      }
      // return false;
   }

   /**
    *
    */
   private static boolean isResultValid(String validationResult, String accno, String domain) {
      if (validationResult.indexOf(" valid " + domain) != -1) {
         if (!cachedValidations.containsKey(domain + accno)) {
            cachedValidations.put(domain + accno, validationResult);
         }
         return true;
      } else {
         return false;
      }
   }

   /**
    * 'onlineValidate' using EB-eye
    */
   // TODO to implement SRP
   // private static String ebEyeValidate(String db, String accno) throws RemoteException, ServiceException {
   public static String ebEyeValidate(String db, String accno) throws RemoteException, ServiceException {
      String query = "acc:\"" + accno + "\" OR id:\"" + accno + "\"";
      // http://www.ebi.ac.uk/ebisearch/ws/rest/interpro?query=acc:%22IPR018060%22%20OR%20id:%22IPR018060%22&format=json
      DomainResult rootDomain = ebeye.getDetailledNumberOfResults(db, query, true);
      String result = accno + " " + db + "" + printDomainResult(rootDomain, "");
      LOGGER.info("ONLINE: " + result);

      return result;
   }

   /** Recursive method to print tree of domain results. 
   * 
   * @param domainDes Domain result node from tree.
   * @param indent Prefix string providing indent for this level in the tree.
   */
   private static String printDomainResult(DomainResult domainRes, String indent) {
      String result = new String();

      if (domainRes.getNumberOfResults().intValue() > 0) { // ???
         result = indent + " valid " + domainRes.getDomainId().getValue();
      } else {
         result = indent + " invalid " + domainRes.getDomainId().getValue();
      }
      return result;
   }

   static {     

      try {
         loadConfigurationFile();
         loadDOIPrefix();
         loadPredefinedResults();

         Nfa anfa = new Nfa(Nfa.NOTHING);
         anfa.or(Xml.GoofedElement("z:acc"), procAccession); // TODO from property
         dfa_access = anfa.compile(DfaRun.UNMATCHED_COPY);

         Nfa bnfa = new Nfa(Nfa.NOTHING);
         bnfa.or(Xml.GoofedElement(prop.getProperty("boundary")), procBoundary); // TODO explore with <article>, <text> and <SENT>?
         dfa_boundary = bnfa.compile(DfaRun.UNMATCHED_COPY);

      } catch (Exception e) {
         LOGGER.log(Level.INFO, "context", e);
      }

   }

   public static void main(String[] arg) throws IOException {
      int port = 7811;
      int j = 0;
      Boolean stdpipe = false;

      try {
         if (arg.length > 0) {   
            port = Integer.parseInt(arg[0]);
            j = 1;
         }
      } catch (java.lang.NumberFormatException ne) { 
         LOGGER.info(arg[0]);
      }   

      for (int i = j; i < arg.length; i++) {
         if ("-stdpipe".equals(arg[i])) {
            stdpipe = true;
         }
      }
        
      if (stdpipe) {
         ValidateAccessionNumber stice = new ValidateAccessionNumber(System.in, System.out);
         stice.run();
      } else {
         LOGGER.info("ValidateAccessionNumber will listen on " + port + " .");
         try {      
            FilterServiceFactory fsf = new FilterServiceFactory(new ServiceFactory () {
                 public Service createService(InputStream in, OutputStream out, Object params)
                 throws ServiceCreateException {
                    return new ValidateAccessionNumber(in,out);
                 } 
            });
            svr = new TcpServer(new ServerSocket(port), fsf, 50);
            svr.setLogging(System.out); 
            svr.serve(); 
         } catch (java.net.BindException be) {
            LOGGER.warning("Couldn't start server"+be.toString()); 
            System.exit(1);
         } 
      }
   }

   public Exception getException() {
      return null;
   }

   @SuppressWarnings("deprecation")
   public void run() {
      DfaRun dfaRun = new DfaRun(dfa_boundary);
      dfaRun.setIn(in);
      PrintStream outpw = new PrintStream(out);

      try {
         dfaRun.filter(outpw);
      } catch (IOException e) {
         LOGGER.log(Level.INFO, "context", e);
      }
   } 
}
