# EuropePMC-Identifier-Extractor
A java program to extract identifiers such as grant ids, accession numbers etc. in free text

### How to build?

-------
ant jar
-------

### How to use?

##### ERC funding id extraction

------------------------------------------------------------------------------------------------------------------
cat test/ercfunds.txt | \
java -cp lib/monq.jar monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 automata/grants150714.mwt | \
java -cp dist/AccessionNumbers.jar ukpmc.ValidateAccessionNumber -stdpipe
------------------------------------------------------------------------------------------------------------------
