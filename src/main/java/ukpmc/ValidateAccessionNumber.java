package ukpmc;

/**
 * Validate Identifiers
 * Author: Jee-Hyub Kim
 * Looks for tagged elements (e.g., accession numbers, DOIs, funding ids, etc.) and attempts to validate those elements.
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

import monq.jfa.AbstractFaAction;
import monq.jfa.Dfa;
import monq.jfa.DfaRun;
import monq.jfa.Nfa;
import monq.jfa.Xml;
import monq.jfa.ReaderCharSource;

import monq.net.FilterServiceFactory;
import monq.net.Service;
import monq.net.ServiceCreateException;
import monq.net.ServiceFactory;
import monq.net.TcpServer;

import ukpmc.scala.MwtParser;
import ukpmc.scala.MwtAtts;

@SuppressWarnings("serial")
public class ValidateAccessionNumber implements Service {

   private static final Logger LOGGER = Logger.getLogger(ValidateAccessionNumber.class.getName());

   private static Properties prop = new Properties();
   private static DoiResolver dr = new DoiResolver();
   private static AccResolver ar = new AccResolver();

   protected static Dfa dfa_boundary = null;
   private static Dfa dfa_plain = null;
   private static Dfa dfa_entity = null;
   
   private static Map<String, String> cachedValidations = new HashMap<String, String>();
   private static Map<String, String> BlacklistDoiPrefix = new HashMap<>();
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
      if (url == null) { throw new RuntimeException("can not find validate.properties!"); }
      prop.load(url.openStream());
   }

   /**
    * Read the stored list of predefined results and fill a cache
    * Note that nothing enforces that the file defines a MAP (there could
    * be more than entry for the same KEY).But this code will just
    * overwrite earlier entries with later ones if this happens.
    * @throws IOException
    */
   private static void loadPredefinedResults() throws IOException {
      String predefFilename;
      predefFilename = prop.getProperty("cached");
      URL pURL = ValidateAccessionNumber.class.getResource("/" + predefFilename);

      BufferedReader reader = new BufferedReader(new InputStreamReader(pURL.openStream()));

      String line;
      while ((line = reader.readLine()) != null) {
         if (line.indexOf("#") != 0) {
            int firstSpace = line.indexOf(" ");
            int secSpace = line.indexOf(" ", firstSpace + 1);
            String accNo = line.substring(0, firstSpace);
            String db = line.substring(firstSpace + 1, secSpace);
            cachedValidations.put(db + accNo, line);
         }
      }
      reader.close();
   }
   
   /**
    * Read the stored list of DOI prefixes for articles only
    *
    */
   private static void loadDOIPrefix() throws IOException {
      // http://stackoverflow.com/questions/27360977/how-to-read-files-from-resources-folder-in-scala
      String doiPrefixFilename = prop.getProperty("doiblacklist");
      URL pURL = ValidateAccessionNumber.class.getResource("/" + doiPrefixFilename);
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

   /**
    *
    */
   private static String reEmbedContent(String taggedF, StringBuilder yytext, Map<String, String> map, int start) {
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
      public void invoke(StringBuilder yytext, int start, DfaRun runner) {
         numOfAccInBoundary = new HashMap<>();
         try {
            Map <String, String> map = Xml.splitElement(yytext, start);
            String content = map.get(Xml.CONTENT);
            String newoutput;

            if ("TABLE".equals(map.get("type"))) {
                DfaRun dfaRunEntity = new DfaRun(dfa_entity);
                dfaRunEntity.clientData = map.get("type");
                newoutput = dfaRunEntity.filter(content);
            } else if ("SENT".equals(map.get(Xml.TAGNAME))) {
                DfaRun dfaRunPlain = new DfaRun(dfa_plain);
                dfaRunPlain.clientData = map.get(Xml.TAGNAME);
                newoutput = dfaRunPlain.filter(content);
            } else {
                DfaRun dfaRunPlain = new DfaRun(dfa_plain);
                dfaRunPlain.clientData = map.get("type");
                newoutput = dfaRunPlain.filter(content);
            }

            String embedContent = reEmbedContent(newoutput, yytext, map, start);
            yytext.replace(start, yytext.length(), embedContent);
         } catch (Exception e) {
            LOGGER.log(Level.INFO, "context", e);
         }
      }
   };

    /**
     *
     */
   private static AbstractFaAction procPlain = new AbstractFaAction() {
      public void invoke(StringBuilder yytext, int start, DfaRun runner) {
         numOfAccInBoundary = new HashMap<>();
         try {
            Map <String, String> map = Xml.splitElement(yytext, start);
            String content = map.get(Xml.CONTENT);

            DfaRun dfaRunEntity = new DfaRun(dfa_entity);
            dfaRunEntity.clientData = runner.clientData; // SENT or SecTag type=xxx

            String newoutput = dfaRunEntity.filter(content);
            String embedContent = reEmbedContent(newoutput, yytext, map, start);
            yytext.replace(start, yytext.length(), embedContent);
         } catch (Exception e) {
            LOGGER.log(Level.INFO, "context", e);
         }
      }
   };


   /**
    *  This processes an accession number
    *  noval: refseq, refsnp, context: eudract offline: pfam, online (+ offline): the rest
    */   
   private static AbstractFaAction procEntity = new AbstractFaAction() {
      public void invoke(StringBuilder yytext, int start, DfaRun runner) {
         try {
            Map<String, String> map = Xml.splitElement(yytext, start);
            MwtAtts m = new MwtParser(map).parse();
	        String textBeforeEntity = getTextBeforeEntity(yytext, start, m.wsize());

            boolean isValid = false;
	        if ("noval".equals(m.valmethod())) {
	            isValid = true;
            } else if ("contextOnly".equals(m.valmethod())) {
               if (isAnySameTypeBefore(m.db()) || isInContext(textBeforeEntity, m.ctx())) isValid = true;
            } else if ("cachedWithContext".equals(m.valmethod())) {
               if ((isAnySameTypeBefore(m.db()) || isInContext(textBeforeEntity, m.ctx())) && isCachedValid(m.db(), m.content(), m.domain())) isValid = true;
            } else if ("onlineWithContext".equals(m.valmethod())) {
               if ((isAnySameTypeBefore(m.db()) || isInContext(textBeforeEntity, m.ctx())) && isOnlineValid(m.db(), m.content(), m.domain())) isValid = true;
            } else if ("context".equals(m.valmethod())) {
               if (isInContext(textBeforeEntity, m.ctx())) isValid = true;
            } else if ("cached".equals(m.valmethod())) {
               if (isCachedValid(m.db(), m.content(), m.domain())) isValid = true;
            } else if ("online".equals(m.valmethod())) {
               if (isOnlineValid(m.db(), m.content(), m.domain())) isValid = true;
            }

            String secOrSent = runner.clientData.toString();
            if (isValid && isInValidSection(secOrSent, m.sec())) {
                String tagged = "<" + m.tagname() +" db=\"" + m.db() + "\" ids=\"" + m.content() +"\">"+ m.content()
                        + "</" + m.tagname() + ">";
                yytext.replace(start, yytext.length(), tagged);
                numOfAccInBoundary.put(m.db(), 1);
            } else { // not valid
                yytext.replace(start, yytext.length(), m.content());
	        }
         } catch (Exception e) {
            LOGGER.log(Level.INFO, "context", e);
         }
      }
   };

    /**
     *
     * @param db
     * @return
     */
   private static boolean isAnySameTypeBefore(String db) { // Can I use this for a range?
      return numOfAccInBoundary.containsKey(db);
   }

    /**
     *
     * @param textBeforeEntity
     * @param context
     * @return
     */
   private static boolean isInContext(String textBeforeEntity, String context) {
       Pattern p = Pattern.compile(context);
       Matcher m = p.matcher(textBeforeEntity);
       return m.find();
   }

    /**
     *
     * @param yytext
     * @param start
     * @param windowSize
     * @return
     */
    private static String getTextBeforeEntity(StringBuilder yytext, int start, Integer windowSize) {
        Integer prevStart = (start - windowSize) < 0 ? 0 : (start - windowSize);
        return yytext.substring(prevStart, start);
    }

    /**
     *
     * @param secOrSent
     * @param blacklistSection
     * @return
     */
   private static boolean isInValidSection(String secOrSent, String blacklistSection) {
      if (blacklistSection.equals("")) {
          return true;
      } else if (secOrSent.contains(blacklistSection)) {
          return false;
      } else {
          return true;
      }
   }

    /**
     *
     * @param db
     * @param id
     * @param domain
     * @return
     */
   static boolean isCachedValid(String db, String id, String domain) {
      id = normalizeID(db, id);
      boolean isValid = false;
      if (cachedValidations.containsKey(domain + id)) {
          String res = cachedValidations.get(domain + id);
          if (res.indexOf(" valid " + domain) != -1) isValid = true;
      }
      return isValid;
   }

   /**
    *
    * pdb and uniprot is case-insensitive, but ENA is upper-case
    */
   private static boolean isOnlineValid(String db, String id, String domain) {
      id = normalizeID(db, id);
      if ("doi".equals(db)) {
         return isDOIValid(id);
      } else {
         return isAccValid(domain, id);
      }
   }

    /**
     *
     * @param doi
     * @return
     */
   public static boolean isDOIValid(String doi) {
      if (BlacklistDoiPrefix.containsKey(prefixDOI(doi))) {
         return false;
      } else if ("10.2210/".equals(doi.substring(0, 8))) { // exception rule for PDB data center
         return true;
      } else if (dr.isValidID("doi", doi)) {
         return true;
      } else {
         return false;
      }
   }

    /**
     *
     * @param domain
     * @param accno
     * @return
     */
   public static boolean isAccValid(String domain, String accno) {
      if (ar.isValidID(domain, accno)) {
        return true;
      } else {
        return false;
      }
   }

   /**
    * normalize accession numbers for cached and online validation
    */
   static String normalizeID(String db, String id) {
       int dotIndex;
       dotIndex = id.indexOf(".");
       if (dotIndex != -1 && !"doi".equals(db)) id = id.substring(0, dotIndex);
       if (id.endsWith(")")) id = id.substring(0, id.length() - 1);
       return id.toUpperCase();
   }

   /**
    * return a prefix of a DOI
    */
   public static String prefixDOI(String doi) {
      String prefix = "";
      int bsIndex = doi.indexOf("/");
      if (bsIndex != -1) prefix = doi.substring(0, bsIndex);
      return prefix;
   }

   static {
      try {
         loadConfigurationFile();
         loadDOIPrefix();
         loadPredefinedResults();

         Nfa bnfa = new Nfa(Nfa.NOTHING);
         bnfa.or(Xml.GoofedElement("SecTag"), procBoundary)
         .or(Xml.GoofedElement("SENT"), procBoundary);
         dfa_boundary = bnfa.compile(DfaRun.UNMATCHED_COPY);

         Nfa snfa = new Nfa(Nfa.NOTHING);
         snfa.or(Xml.GoofedElement("plain"), procPlain);
         dfa_plain = snfa.compile(DfaRun.UNMATCHED_COPY);

         Nfa anfa = new Nfa(Nfa.NOTHING);
         anfa.or(Xml.GoofedElement(prop.getProperty("entity")), procEntity);
         dfa_entity = anfa.compile(DfaRun.UNMATCHED_COPY);
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
         ValidateAccessionNumber validator = new ValidateAccessionNumber(System.in, System.out);
         validator.run();
      } else {
         LOGGER.info("ValidateAccessionNumber will listen on " + port + " .");
         try {      
            FilterServiceFactory fsf = new FilterServiceFactory(new ServiceFactory() {
                 public Service createService(InputStream in, OutputStream out, Object params)
                 throws ServiceCreateException {
                    return new ValidateAccessionNumber(in, out);
                 }
            });

            TcpServer svr = new TcpServer(new ServerSocket(port), fsf, 50);
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
      dfaRun.setIn(new ReaderCharSource(in));
      PrintStream outpw = new PrintStream(out);

      try {
         dfaRun.filter(outpw);
      } catch (IOException e) {
         LOGGER.log(Level.INFO, "context", e);
      }
   } 
}
