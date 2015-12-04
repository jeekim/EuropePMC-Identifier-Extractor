package ukpmc;

/**
 * Validate Accession Number spotting
 * 
 * Author: Jee-Hyub Kim
 *
 * Looks for tagged elements and attempts to validate the accession number
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

import ukpmc.scala.MwtParser;
import ukpmc.scala.MwtAtts;

@SuppressWarnings("serial")
public class ValidateAccessionNumber implements Service {


   private static final Logger LOGGER = Logger.getLogger(ValidateAccessionNumber.class.getName()); 

   private static TcpServer svr = null;

   private static Properties prop = new Properties();
   private static DoiResolver dr = new DoiResolver();
   private static AccResolver ar = new AccResolver();
   
   protected static Dfa dfa_boundary = null;
   private static Dfa dfa_entity = null;
   
   private static Map<String, String> cachedValidations = new HashMap<String, String>();
   private static Map<String, String> cachedDoiPrefix = new HashMap<String, String>();
   private static Map<String, Integer> numOfAccInBoundary = new HashMap<String, Integer>();
   // Integer for position? Integer => Mention(Position, Acc)
   // <z:acc db="omim" ids="603878-603890">603878 to 603890</z:acc>
 
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
      // http://stackoverflow.com/questions/27360977/how-to-read-files-from-resources-folder-in-scala
      String doiprefixfilename = prop.getProperty("doiblacklist");
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
    *
    */
   private static AbstractFaAction procBoundary = new AbstractFaAction() {
      public void invoke(StringBuffer yytext, int start, DfaRun runner) {
	 // when a boundary in a table, expands the boundary
         numOfAccInBoundary = new HashMap<String, Integer>();
         try {
            Map <String, String> map = Xml.splitElement(yytext, start);
            String xmlcontent = map.get(Xml.CONTENT);
            DfaRun dfaRunEntity = new DfaRun(dfa_entity);
            String newoutput = dfaRunEntity.filter(xmlcontent);
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
   private static AbstractFaAction procEntity = new AbstractFaAction() {
      public void invoke(StringBuffer yytext, int start, DfaRun runner) {
         LOGGER.setLevel(Level.SEVERE);
         try { 
            Map<String, String> map = Xml.splitElement(yytext, start);
	    MwtAtts m = new MwtParser(map).parse();

	    boolean useTagged = false; // TODO can I use Option or pattern matching here? get the decision from the case class.
            if ("noval".equals(m.valmethod())) {
	       useTagged = true;
            } else if ("contextOnly".equals(m.valmethod())) {
               if (isAnySameTypeBefore(m.db()) || isInContext(yytext, start, m.context(), m.wsize())) {
	          useTagged = true;
               }
            } else if ("cachedWithContext".equals(m.valmethod())) {
               if ((isAnySameTypeBefore(m.db()) || isInContext(yytext, start, m.context(), m.wsize())) && isCachedValid(m.db(), m.xmlcontent(), m.domain())) {
	          useTagged = true;
		}
            } else if ("onlineWithContext".equals(m.valmethod())) {
               if ((isAnySameTypeBefore(m.db()) || isInContext(yytext, start, m.context(), m.wsize())) && isOnlineValid(m.db(), m.xmlcontent(), m.domain())) {
	          useTagged = true;
               }
            } else if ("cached".equals(m.valmethod())) {
                  if (isCachedValid(m.db(), m.xmlcontent(), m.domain())) {
	             useTagged = true;
                  }
            } else if ("online".equals(m.valmethod())) {
                  if (isOnlineValid(m.db(), m.xmlcontent(), m.domain())) {
	             useTagged = true;
                  }
            }

	    if (useTagged) {
              String tagged = "<" + m.tagname() +" db=\"" + m.db() + "\" ids=\"" + m.xmlcontent() +"\">" + m.xmlcontent() + "</" + m.tagname() + ">";
              numOfAccInBoundary.put(m.db(), 1);
              yytext.replace(start, yytext.length(), tagged);
	    } else {
              yytext.replace(start, yytext.length(), m.xmlcontent()); // no tagging
	    }

         } catch (Exception e) {
            LOGGER.log(Level.INFO, "context", e);
         }
      }
   };

   /**
    *
    */
   private static boolean isAnySameTypeBefore(String db) { // Can I use this for a range?
      if ("erc".equals(db)) { return false; }
      return numOfAccInBoundary.containsKey(db);
   }
   
   /**
    *
    */
   private static boolean isInContext(StringBuffer yytext, int start, String context, String wsize) {
      Integer wSize = Integer.parseInt(wsize);
      Integer pStart = start - wSize;
      if (pStart < 0) { pStart = 0; }
      Pattern p = Pattern.compile(context);
      Matcher m = p.matcher(yytext.substring(pStart, start));
      return m.find();
   }

   /**
    *
    *
    */
   public static boolean isCachedValid(String db, String accno, String domain) {
      accno = normalizeID(db, accno);

      if (cachedValidations.containsKey(domain + accno)) {
        String res = cachedValidations.get(domain + accno);
        if (res.indexOf(" valid " + domain) != -1) {
           return true;
        } else { // " invalid "
           return false;
        }
      } else {
        return false;
      }
   }

   /**
    * pdb and uniprot is case-insensitive, but ENA is upper-case
    */
   public static boolean isOnlineValid(String db, String id, String domain) {
      id = normalizeID(db, id);

      if ("doi".equals(db)) { // if id is a doi
         return isDOIValid(id);
      } else {
         return isAccValid(domain, id);
	 // return ar.isValidID(domain, id);
      }
   }

   /**
    *
    */
   public static boolean isDOIValid(String doi) {
      if (cachedDoiPrefix.containsKey(prefixDOI(doi))) {
         LOGGER.info(doi + ": in the black list.");
         return false;
      } else if ("10.2210/".equals(doi.substring(0, 8))) { // exception rule for PDB data center
         LOGGER.info(doi + ": in PDB data center.");
         return true;
      } else if (dr.isValidID("doi", doi)) {
         LOGGER.info(doi + ": is a valid id.");
         return true;
      } else {
         LOGGER.info(doi + ": is not a valid id.");
         return false;
      }
   }

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
   public static String normalizeID(String db, String id) {
      int dotIndex = id.indexOf("."); // if it's a dotted Accession number, then only test the prefix

      if (dotIndex != -1 && !"doi".equals(db)) { id = id.substring(0, dotIndex); }
      if (")".equals(id.substring(id.length() - 1))) { id = id.substring(0, id.length() - 1); }

      return id.toUpperCase();
   }

   /**
    * return a prefix of a DOI
    */
   public static String prefixDOI(String doi) {
      String prefix = new String();
      int bsIndex = doi.indexOf("/");

      if (bsIndex != -1) {
        prefix = doi.substring(0, bsIndex);
      }           
      return prefix;
   }


   static {     

      try {
         loadConfigurationFile();
         loadDOIPrefix();
         loadPredefinedResults();

         Nfa anfa = new Nfa(Nfa.NOTHING);
         anfa.or(Xml.GoofedElement(prop.getProperty("entity")), procEntity);
         dfa_entity = anfa.compile(DfaRun.UNMATCHED_COPY);

	 // <z:acc db="%1" valmethod="%2" domain="%3" context="%4" wsize="%5" boundary="???">
         Nfa bnfa = new Nfa(Nfa.NOTHING);
         bnfa.or(Xml.GoofedElement(prop.getProperty("boundary")), procBoundary);
	 // .or ... table ... <table> ... <s> </s> <s> </s> ... </table>
         dfa_boundary = bnfa.compile(DfaRun.UNMATCHED_COPY);
	  
         LOGGER.warning(prop.getProperty("boundary"));

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
