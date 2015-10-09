# Identifier Extractor

A text-mining pipeline to extract identifiers such as grant ids, accession numbers etc. in free text. The pipeline mainly consists of two java programs.

 1. Dictionary-based tagger. Given a dictionary, the tagger identifies terms in the dictionary using a Java Finite Automata library. 
 2. Validator. For each identified term, the validator removes erribeiys terms using several mechanisms (contextual information, online validation, etc.).

### How to build?


```
ant jar
```

### How to use?

You need to create a dictionary based on mwt format and format your input documents in xml.

#### MWT dictionary format

```
<mwt>
  <template><$TAG db="%1" valmethod="%2" domain="%3" context="%4" wsize="%5">%0<$TAG></template>
  <r p1="$DBNAME" p2="$VALMETHOD" p3="$DOMAIN" p4="$CONTEXT" p5="$WINDOW_SIZE">$PATTERN</r>
</mwt>
```

#### Input document format

The pipeline takes sentences as input. Those sentences have to be formatted as follows>

```
<article>
  <text>
    <SENT sid="0" pm="."><plain>$FIRST_SENTENCE</plain></SENT>
    <SENT sid="1" pm="."><plain>$SECOND_SENTENCE</plain></SENT>
  </text>
</article>
```

#### Examples

##### European Research Council funding id extraction

```
cat test/ercfunds.txt | \
java -cp lib/monq.jar monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 automata/grants150714.mwt | \
java -cp dist/AccessionNumbers.jar ukpmc.ValidateAccessionNumber -stdpipe
```

##### Accession number extraction

```
cat test/accession.txt | \
java -cp lib/monq.jar monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 automata/acc150612.mwt | \
java -cp dist/AccessionNumbers.jar ukpmc.ValidateAccessionNumber -stdpipe
```

### Acknowledgements

This work was supported by Europe PMC funders (for accession number extraction) and European Research Council (for grant id extraction).


[1]: http://europepmc.org/articles/PMC3667078
[2]: http://europepmc.org/abstract/MED/18006544
