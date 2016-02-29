#!/bin/sh
# a script to fetch, annotate, transform, and summarize, for xml, ocr, and pdf
# input: stdin
# output: stdout

# paths
UKPMC=/nfs/gns/literature/textmining/ePMC
UKPMCXX=$UKPMC/lib
DICXX=$UKPMC/automata

JAVA_HOME=/nfs/misc/literature/tools/java/jdk1.7.0_01_x64

OTHERS=$UKPMCXX/ebitmjimenotools.jar:$UKPMCXX/monq.jar:$UKPMCXX/mallet.jar:$UKPMCXX/mallet-deps.jar:$UKPMCXX/marie.jar:$UKPMCXX/pipeline150716.jar:$UKPMCXX/commons-lang-2.4.jar:$UKPMCXX/ojdbc6-11.1.0.7.0.jar:$UKPMCXX/ie.jar

# commands
# works as stdin and stdout
STDERR=$1

ADDTEXT="$JAVA_HOME/bin/java -XX:+UseSerialGC -cp $OTHERS:$UKPMCXX/pmcxslpipe150818.jar ebi.ukpmc.xslpipe.Pipeline -stdpipe -stageSpotText"
OUTTEXT="$JAVA_HOME/bin/java -XX:+UseSerialGC -cp $OTHERS:$UKPMCXX/pmcxslpipe150818.jar ebi.ukpmc.xslpipe.Pipeline -stdpipe -outerText"
REMBACK="$JAVA_HOME/bin/java -XX:+UseSerialGC -cp $OTHERS:$UKPMCXX/pmcxslpipe150818.jar ebi.ukpmc.xslpipe.Pipeline -stdpipe -removeBackPlain -fixEBIns"

SENTENCISER="$JAVA_HOME/bin/java -XX:+UseSerialGC -cp $UKPMCXX/Sentenciser.jar ebi.ukpmc.sentenciser.Sentencise -rs '<article[^>]+>' -ok -ie UTF-8 -oe UTF-8"
SENTCLEANER="$JAVA_HOME/bin/java -XX:+UseSerialGC -cp $UKPMCXX/Sentenciser.jar ebi.ukpmc.sentenciser.SentCleaner -stdpipe"
SEC_TAG="$UKPMC/bin/SectionTagger_XML_inline.pl"
RDF_GEN="$UKPMC/bin/gen_ttl.pl"

SP_DICT="$JAVA_HOME/bin/java -XX:+UseSerialGC -Xmx3000m -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=10 -cp $OTHERS monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 $DICXX/swissprot_Sept2014.mwt"
OR_DICT="$JAVA_HOME/bin/java -XX:+UseSerialGC -Xmx13000m -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=10 -cp $OTHERS monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 $DICXX/Organisms150507.mwt"
GO_DICT="$JAVA_HOME/bin/java -XX:+UseSerialGC -Xmx3000m -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=10 -cp $OTHERS monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 $DICXX/go150429.mwt"
DI_DICT="$JAVA_HOME/bin/java -XX:+UseSerialGC -Xms5G -Xmx5G -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=10 -cp $OTHERS monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 $DICXX/DiseaseDictionary.mwt"
CH_DICT="$JAVA_HOME/bin/java -XX:+UseSerialGC -Xms5000m -Xmx6G -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=10 -cp $OTHERS monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 $DICXX/chebi150615_wo_role.mwt"
EFO_DICT="$JAVA_HOME/bin/java -XX:+UseSerialGC -Xmx1000m -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=10 -cp $OTHERS monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 $DICXX/efo150428.mwt"
AC_DICT="$JAVA_HOME/bin/java -XX:+UseSerialGC -Xms1000m -Xmx1G -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=10 -cp $OTHERS monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 $DICXX/acc150612.mwt"
GR_DICT="$JAVA_HOME/bin/java -XX:+UseSerialGC -Xms1000m -Xmx1G -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=10 -cp $OTHERS monq.programs.DictFilter -t elem -e text4fund -ie UTF-8 -oe UTF-8 $DICXX/grants150714.mwt"

BNCFILTER="$JAVA_HOME/bin/java -XX:+UseSerialGC -cp $OTHERS -Xmx400m -XX:MinHeapFreeRatio=15 -XX:MaxHeapFreeRatio=15 marie.bnc.BncFilter 160"
CHEBIBNCFILTER="$JAVA_HOME/bin/java -XX:+UseSerialGC -cp $OTHERS -Xmx400m -XX:MinHeapFreeRatio=15 -XX:MaxHeapFreeRatio=15 ebi.ukpmc.pipeline.filter.ChEBIBncFilter 250"

CH_FILTER="$JAVA_HOME/bin/java -XX:+UseSerialGC -cp $UKPMCXX/chebifilter150615.jar:$OTHERS ebi.ukpmc.chebi.ChEBIFilter"
OR_FILTER="$JAVA_HOME/bin/java -XX:+UseSerialGC -cp $UKPMCXX/organismsfilter150506.jar:$OTHERS -Xmx1024m -Xms1024m ebi.ukpmc.organisms.OrganismsFilter"

ACC_VAL="$JAVA_HOME/bin/java -XX:+UseSerialGC -cp $UKPMCXX/commons-cli-1.2.jar:$UKPMCXX/EBeye_JAXWS.jar:$UKPMCXX/AccessionNumbers150613.jar:$OTHERS ukpmc.EBeyeValidateAccessionNumber -stdpipe"

WHATIZIT2IEXML="$JAVA_HOME/bin/java -XX:+UseSerialGC -Xmx512M -Xms512M -cp $OTHERS ebi.ukpmc.pipeline.normalize.whatizit2iexml"
NORMALIZEIDS="$JAVA_HOME/bin/java -XX:+UseSerialGC -cp $OTHERS ebi.ukpmc.pipeline.normalize.NormalizeIds"

XML_4INDEX="$JAVA_HOME/bin/java -XX:+UseSerialGC -cp $OTHERS ebi.ukpmc.pipeline.xslt.Pipeline -xml2index"
XML_SUMMARY="$JAVA_HOME/bin/java -XX:+UseSerialGC -cp $OTHERS ebi.ukpmc.pipeline.xslt.Pipeline -xml2summary"
XML_OA="$JAVA_HOME/bin/java -XX:+UseSerialGC -cp $OTHERS ebi.ukpmc.pipeline.xslt.Pipeline -xml2oa"
OCR_4INDEX="$JAVA_HOME/bin/java -XX:+UseSerialGC -cp $OTHERS ebi.ukpmc.pipeline.xslt.Pipeline -ocr2index"
OCR_SUMMARY="$JAVA_HOME/bin/java -XX:+UseSerialGC -cp $OTHERS ebi.ukpmc.pipeline.xslt.Pipeline -ocr2summary"
FETCH="$JAVA_HOME/bin/java -XX:+UseSerialGC -cp $OTHERS -Doracle.net.tns_admin=/sw/common/oracle/ ebi.ukpmc.pipeline.fetch.FetchCDB"

# pipelines, this is based on the pipeline diagram
if [ "$1" = "help" ]; then
  echo "usage: program error_log mode file_format"

# fetch
elif [ "$2" = "fetch" ] && [ "$3" = "xml" ]; then
  $FETCH $3 $4 $5 $6 $7 $8 $9 ${10} 2>> $STDERR
elif [ "$2" = "fetch" ] && [ "$3" = "ocr" ]; then
  $FETCH $3 $4 $5 $6 $7 $8 $9 ${10} 2>> $STDERR
elif [ "$2" = "fetch" ] && [ "$3" = "pdf" ]; then
  $FETCH $3 $4 $5 $6 $7 $8 $9 ${10} 2>> $STDERR

# annotate
elif [ "$2" = "annotation" ] && [ "$3" = "xml" ]; then
  sed 's/"article-type=/" article-type=/' 2>> $STDERR | $SEC_TAG 2>> $STDERR | $ADDTEXT 2>> $STDERR | $OUTTEXT 2>> $STDERR | $SENTENCISER 2>> $STDERR | $GR_DICT 2>> $STDERR | $AC_DICT 2>> $STDERR | $ACC_VAL 2>> $STDERR | $SENTCLEANER 2>> $STDERR | $REMBACK 2>> $STDERR | $SP_DICT 2>> $STDERR | $BNCFILTER 2>> $STDERR | $OR_DICT 2>> $STDERR | $OR_FILTER 2>> $STDERR | $GO_DICT 2>> $STDERR | $DI_DICT 2>> $STDERR | $CH_DICT 2>> $STDERR | $CHEBIBNCFILTER 2>> $STDERR | $CH_FILTER 2>> $STDERR | $EFO_DICT 2>> $STDERR | $SENTCLEANER 2>> $STDERR
elif [ "$2" = "annotation" ] && [ "$3" = "ocr" ]; then
  $SENTENCISER 2>> $STDERR | $AC_DICT 2>> $STDERR | $ACC_VAL 2>> $STDERR | $SP_DICT 2>> $STDERR | $BNCFILTER 2>> $STDERR | $OR_DICT 2>> $STDERR | $OR_FILTER 2>> $STDERR | $GO_DICT 2>> $STDERR | $DI_DICT 2>> $STDERR | $CH_DICT 2>> $STDERR | $CHEBIBNCFILTER 2>> $STDERR | $CH_FILTER 2>> $STDERR | $EFO_DICT 2>> $STDERR
elif [ "$2" = "annotation" ] && [ "$3" = "pdf" ]; then
  $SENTENCISER 2>> $STDERR | $AC_DICT 2>> $STDERR | $ACC_VAL 2>> $STDERR | $SP_DICT 2>> $STDERR | $BNCFILTER 2>> $STDERR | $OR_DICT 2>> $STDERR | $OR_FILTER 2>> $STDERR | $GO_DICT 2>> $STDERR | $DI_DICT 2>> $STDERR | $CH_DICT 2>> $STDERR | $CHEBIBNCFILTER 2>> $STDERR | $CH_FILTER 2>> $STDERR | $EFO_DICT 2>> $STDERR

# 4index
elif [ "$2" = "4index" ] && [ "$3" = "xml" ]; then
  $WHATIZIT2IEXML 2>> $STDERR | $NORMALIZEIDS 2>> $STDERR | $XML_4INDEX 2>> $STDERR
elif [ "$2" = "4index" ] && [ "$3" = "ocr" ]; then
  $WHATIZIT2IEXML 2>> $STDERR | $NORMALIZEIDS 2>> $STDERR | $OCR_4INDEX 2>> $STDERR
elif [ "$2" = "4index" ] && [ "$3" = "pdf" ]; then
  $WHATIZIT2IEXML 2>> $STDERR | $NORMALIZEIDS 2>> $STDERR | $OCR_4INDEX 2>> $STDERR

# 4summary
elif [ "$2" = "4summary" ] && [ "$3" = "xml" ]; then
  $WHATIZIT2IEXML 2>> $STDERR | $NORMALIZEIDS 2>> $STDERR | $XML_SUMMARY 2>> $STDERR | grep -v '^<!DOCTYPE' 2>> $STDERR
elif [ "$2" = "4summary" ] && [ "$3" = "ocr" ]; then
  $WHATIZIT2IEXML 2>> $STDERR | $NORMALIZEIDS 2>> $STDERR | $OCR_SUMMARY 2>> $STDERR | grep -v '^<!DOCTYPE' 2>> $STDERR
elif [ "$2" = "4summary" ] && [ "$3" = "pdf" ]; then
  $WHATIZIT2IEXML 2>> $STDERR | $NORMALIZEIDS 2>> $STDERR | $OCR_SUMMARY 2>> $STDERR | grep -v '^<!DOCTYPE' 2>> $STDERR

# 4oa
elif [ "$2" = "4oa" ] && [ "$3" = "xml" ]; then
  $WHATIZIT2IEXML 2>> $STDERR | $NORMALIZEIDS 2>> $STDERR | $XML_OA 2>> $STDERR | grep -v '^<!DOCTYPE' 2>> $STDERR

# 4rdf
elif [ "$2" = "4rdf" ] && [ "$3" = "xml" ]; then
  $WHATIZIT2IEXML 2>> $STDERR | $NORMALIZEIDS 2>> $STDERR | $XML_OA 2>> $STDERR | grep -v '^<!DOCTYPE' 2>> $STDERR | $RDF_GEN 2>> $STDERR

# tm
elif [ "$2" = "tm" ] && [ "$3" = "xml" ]; then
  $ADDTEXT 2>> $STDERR | $OUTTEXT 2>> $STDERR | $SENTENCISER 2>> $STDERR | $ACC_TAG 2>> $STDERR | $ACC_VAL 2>> $STDERR | $SENTCLEANER 2>> $STDERR | $REMBACK 2>> $STDERR | $SP_DICT 2>> $STDERR | $BNCFILTER 2>> $STDERR | $OR_DICT 2>> $STDERR | $OR_FILTER 2>> $STDERR | $GO_DICT 2>> $STDERR | $DI_DICT 2>> $STDERR | $ABBFILTER 2>> $STDERR | $CH_DICT 2>> $STDERR | $CHEBIBNCFILTER 2>> $STDERR | $CH_FILTER 2>> $STDERR | $EFO_DICT 2>> $STDERR | $SENTCLEANER 2>> $STDERR | $WHATIZIT2IEXML 2>> $STDERR | $NORMALIZEIDS 2>> $STDERR | $XML_SUMMARY 2>> $STDERR | grep -v '^<!DOCTYPE' 2>> $STDERR
elif [ "$2" = "tm" ] && [ "$3" = "ocr" ]; then
  $SENTENCISER 2>> $STDERR | $ACC_TAG 2>> $STDERR | $ACC_VAL 2>> $STDERR | $SP_DICT 2>> $STDERR | $BNCFILTER 2>> $STDERR | $OR_DICT 2>> $STDERR | $OR_FILTER 2>> $STDERR | $GO_DICT 2>> $STDERR | $DI_DICT 2>> $STDERR | $ABBFILTER 2>> $STDERR | $CH_DICT 2>> $STDERR | $CHEBIBNCFILTER 2>> $STDERR | $CH_FILTER 2>> $STDERR | $EFO_DICT 2>> $STDERR | $WHATIZIT2IEXML 2>> $STDERR | $NORMALIZEIDS 2>> $STDERR | $OCR_SUMMARY 2>> $STDERR | grep -v '^<!DOCTYPE' 2>> $STDERR
elif [ "$2" = "tm" ] && [ "$3" = "pdf" ]; then
  $SENTENCISER 2>> $STDERR | $ACC_TAG 2>> $STDERR | $ACC_VAL 2>> $STDERR | $SP_DICT 2>> $STDERR | $BNCFILTER 2>> $STDERR | $OR_DICT 2>> $STDERR | $OR_FILTER 2>> $STDERR | $GO_DICT 2>> $STDERR | $DI_DICT 2>> $STDERR | $ABBFILTER 2>> $STDERR | $CH_DICT 2>> $STDERR | $CHEBIBNCFILTER 2>> $STDERR | $CH_FILTER 2>> $STDERR | $EFO_DICT 2>> $STDERR | $WHATIZIT2IEXML 2>> $STDERR | $NORMALIZEIDS 2>> $STDERR | $OCR_SUMMARY 2>> $STDERR | grep -v '^<!DOCTYPE' 2>> $STDERR
fi

# end
