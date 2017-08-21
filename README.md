# Identifiers Extractor

[![Build Status](https://travis-ci.org/jeekim/EuropePMC-Identifier-Extractor.svg)](https://travis-ci.org/jeekim/EuropePMC-Identifier-Extractor)

A text-mining pipeline to extract identifiers such as European Research Council grant ids in free text. The pipeline mainly consists of two java programs.

 1. (TODO) Dictionary builder. Given a tsv file, build an MWT-based dictionary.
 2. Dictionary-based tagger. Given a dictionary, the tagger identifies terms in the dictionary using a Java Finite Automata library. 
 3. Validator. For each identified term, the validator removes an errorneous term using several mechanisms (contextual information, online validation, etc.).

### How to build?

```
sbt assembly
```

### How to use?

You need to create a dictionary based on mwt format and format your input documents in xml.

#### MWT dictionary format

```
<mwt>
  <template><acc db="%1" valmethod="%2" domain="%3" context="%4" wsize="%5">%0</acc></template>
  <r p1="$DBNAME" p2="$VALMETHOD" p3="$DOMAIN" p4="$CONTEXT" p5="$WINDOW_SIZE">$PATTERN</r>
</mwt>
```

- valmethod: noval (no validation), contextOnly (keyword-based constraints), onlne (validation using online validation), onlineWithContext (keyword-based constraints and online validation using online validation.)
- domain: one of domain identifier mentioned in https://www.ebi.ac.uk/ebisearch/overview.ebi
- context: a list of keywords
- wsize: the size of window on the left side of a matched term

#### How to build a dictionary?

- from owl
- (TODO) from tsv
- (TODO) from identifiers.org

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
sbt testERC

or

cat test/ercfunds.txt | \
java -cp lib/monq-1.7.1.jar monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 automata/grants150714.mwt | \
java -cp target/scala-2.10/europepmc-identifier-extractor-assembly-0.1-SNAPSHOT.jar ukpmc.ValidateAccessionNumber -stdpipe
```

##### Accession number mining

```
sbt testAcc

or

cat test/accnums.txt | \
java -cp lib/monq-1.7.1.jar monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 automata/acc150612.mwt | \
java -cp target/scala-2.10/europepmc-identifier-extractor-assembly-0.1-SNAPSHOT.jar ukpmc.ValidateAccessionNumber -stdpipe
```

##### Running as server

```
java -cp lib/monq-1.7.1.jar monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 automata/acc150612.mwt -p 3333 &
java -cp target/scala-2.10/europepmc-identifier-extractor-assembly-0.1-SNAPSHOT.jar ukpmc.ValidateAccessionNumber &
echo "<SENT><plain>pdb 1aj9</plain></SENT>" | java -cp lib/monq-1.7.1.jar monq.programs.DistFilter -c . 'host=localhost;port=3333' 'host=localhost;port=7811'
```

### TODO

- to implement ! for negation.
- sbt plugin for plug and play.
- to build a container.
- to run as container (e.g., Docker)
- to nun on AWS

### Acknowledgements

This work was supported by European Research Council (H2020 ERC-EuropePMC-2-2014 637529).


[1]: http://europepmc.org/articles/PMC3667078
[2]: http://europepmc.org/abstract/MED/18006544
