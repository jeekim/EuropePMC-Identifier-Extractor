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
import monq.net.TcpServer;

import ukpmc.scala.MwtParser;
import ukpmc.scala.MwtAtts;
import static ukpmc.util.TaggerUtils.reEmbedContent;

public class AnnotationFilter implements Service {

   private static final Logger LOGGER = Logger.getLogger(AnnotationFilter.class.getName());
   private static Properties prop = new Properties();

   private static Resolver dr = new DoiResolver();
   private static Resolver ar = new AccResolver();
   private static Resolver nr = new NcbiResolver();

   protected static Dfa dfa_boundary;
   private static Dfa dfa_plain;
   private static Dfa dfa_entity;
   
   private static Map<String, String> cachedValidations = new HashMap<>(); // TODO to remove
   private static Map<String, Integer> numOfAccInBoundary = new HashMap<>();
   private InputStream in;
   private OutputStream out;

   public AnnotationFilter(InputStream in, OutputStream out) {
      this.in = in;
      this.out = out;
   }

   /**
    * @throws IOException
    * checks that validate.properties exists and load it
    */
   private static void loadConfigurationFile() throws IOException {
      URL url = AnnotationFilter.class.getResource("/validate.properties");
      if (url == null) throw new RuntimeException("can not find validate.properties!");
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
      URL url = AnnotationFilter.class.getResource("/" + predefFilename);

      BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

      String line;
      while ((line = reader.readLine()) != null) {
         if (!line.startsWith("#")) {
            int firstSpace = line.indexOf(" ");
            int secondSpace = line.indexOf(" ", firstSpace + 1);
            String accNo = line.substring(0, firstSpace);
            String db = line.substring(firstSpace + 1, secondSpace);
            cachedValidations.put(db + accNo, line);
         }
      }
      reader.close();
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

          /* if ("TABLE".equals(map.get("type"))) {
              DfaRun dfaRunEntity = new DfaRun(dfa_entity);
              dfaRunEntity.clientData = map.get("type");
              newoutput = dfaRunEntity.filter(content);
          } else */ if ("SENT".equals(map.get(Xml.TAGNAME))) {
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
	        if ("noval".equals(m.valMethod())) {
	            isValid = true;
            } else if ("contextOnly".equals(m.valMethod())) {
               if (isAnySameTypeBefore(m.db()) || isInContext(textBeforeEntity, m.ctx())) isValid = true;
            } else if ("cachedWithContext".equals(m.valMethod())) {
               if ((isAnySameTypeBefore(m.db()) || isInContext(textBeforeEntity, m.ctx())) && isIdValidInCache(m.db(),
                       m.content(), m.domain())) isValid = true;
            } else if ("onlineWithContext".equals(m.valMethod())) {
               if ((isAnySameTypeBefore(m.db()) || isInContext(textBeforeEntity, m.ctx())) && isOnlineValid(m.db(),
                       m.content(), m.domain())) isValid = true;
            } else if ("context".equals(m.valMethod())) {
               if (isInContext(textBeforeEntity, m.ctx())) isValid = true;
            } else if ("cached".equals(m.valMethod())) {
               if (isIdValidInCache(m.db(), m.content(), m.domain())) isValid = true;
            } else if ("online".equals(m.valMethod())) {
               if (isOnlineValid(m.db(), m.content(), m.domain())) isValid = true;
            }

            String secOrSent = runner.clientData.toString();
            if (isValid && isInValidSection(secOrSent, m.sec())) {
                String tagged = "<" + m.tagName() +" db=\"" + m.db() + "\" ids=\"" + m.content() +"\">"+ m.content()
                        + "</" + m.tagName() + ">";
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
      } else return !secOrSent.contains(blacklistSection);
   }

    /**
     *
     * @param db
     * @param id
     * @param domain
     * @return
     */
   static boolean isIdValidInCache(String db, String id, String domain) {
      id = ar.normalizeID(db, id);
      boolean isValid = false;
      if (cachedValidations.containsKey(domain + id)) {
          String res = cachedValidations.get(domain + id);
          if (res.contains(" valid " + domain)) isValid = true;
      }
      return isValid;
   }

   /**
    *
    * pdb and uniprot is case-insensitive, but ENA is upper-case
    */
   // TODO test this method. is it the right place?
   static boolean isOnlineValid(String db, String id, String domain) {
      // id = ar.normalizeID(db, id);
      if ("doi".equals(db)) {
         return dr.isValid("doi", id);
      } else if ("refseq".equals(db)) {
            return nr.isValid("nucleotide", id);
      } else if ("refsnp".equals(db)) {
         return nr.isValid("snp", id);
      } else if ("gca".equals(db)) {
          return ar.isValid("genome_assembly", id);
      } else {
         id = ar.normalizeID(db, id);
         return ar.isValid(domain, id);
      }
   }

   static {
      try {
         loadConfigurationFile();
         loadPredefinedResults();

         Nfa bnfa = new Nfa(Nfa.NOTHING);
         bnfa // .or(Xml.GoofedElement("SecTag"), procBoundary)
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
         ;
         // LOGGER.info(arg[0]);
      }

      for (int i = j; i < arg.length; i++) {
         if ("-stdpipe".equals(arg[i])) {
            stdpipe = true;
         }
      }
        
      if (stdpipe) {
         AnnotationFilter validator = new AnnotationFilter(System.in, System.out);
         validator.run();
      } else {
         // LOGGER.info("AnnotationFilter will listen on " + port + " .");
         try {      
            FilterServiceFactory fsf = new FilterServiceFactory(
                    (in, out, params) -> new AnnotationFilter(in, out));

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
