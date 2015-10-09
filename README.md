# Identifier Extractor

A text-mining pipeline to extract identifiers such as grant ids, accession numbers etc. in free text. The pipeline consists of two java programs.

 1. Dictionary-based tagger: 
 2. Validator

### How to build?


```
ant jar
```

### How to use?

#### Dictionary format

You need to build a dictionary file in mwt format as follows.

```
<mwt>
  <template><$TAG db="%1" valmethod="%2" domain="%3" context="%4" wsize="%5">%0<$TAG></template>
  <r p1="$DBNAME" p2="$VALMETHOD" p3="$DOMAIN" p4="$CONTEXT" p5="$WINDOW_SIZE">$PATTERN</r>
</mwt>
```

#### Input document format

#### Examples

##### European Research Council funding id extraction

```
cat test/ercfunds.txt | \
java -cp lib/monq.jar monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 automata/grants150714.mwt | \
java -cp dist/AccessionNumbers.jar ukpmc.ValidateAccessionNumber -stdpipe
```

##### Accession number mining

```
cat test/accession.txt | \
java -cp lib/monq.jar monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 automata/acc150612.mwt | \
java -cp dist/AccessionNumbers.jar ukpmc.ValidateAccessionNumber -stdpipe
```
