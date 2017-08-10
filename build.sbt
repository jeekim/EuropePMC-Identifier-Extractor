// import AssemblyKeys._
import sbt.complete.DefaultParsers._

// assemblySettings

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"
libraryDependencies += "org.specs2" %% "specs2-core" % "3.6.6" % "test"
libraryDependencies += "org.specs2" %% "specs2-gwt" % "3.6.6" % "test"
libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.12.5" % "test"

scalacOptions in (Compile,doc) := Seq("-groups", "-implicits")
scalacOptions in Test ++= Seq("-Yrangepos")

lazy val root = (project in file("."))
  .settings(
    name := "AnnotationFilter",
    version := "v1.1"
  )

lazy val testPMC = taskKey[Unit]("Prints 'PMC test results'")
testPMC := {
  "cat corpora/PMC4969258_PMC4986126.xml" #|
  "java -XX:+UseSerialGC -cp lib/monq-1.7.1.jar:lib/pmcxslpipe.jar ebi.ukpmc.xslpipe.Pipeline -stdpipe -stageSpotText" #|
  "java -XX:+UseSerialGC -cp lib/monq-1.7.1.jar:lib/pmcxslpipe.jar ebi.ukpmc.xslpipe.Pipeline -stdpipe -outerText" #|
  "java -XX:+UseSerialGC -cp lib/Sentenciser.jar ebi.ukpmc.sentenciser.Sentencise -rs '<article[^>]+>' -ok -ie UTF-8 -oe UTF-8" #|
  "java -cp lib/monq-1.7.1.jar monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 automata/acc170731.mwt" #|
  "java -cp lib/monq-1.7.1.jar monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 automata/resources170731.mwt" #|
  "java -cp target/scala-2.10/AnnotationFilter-assembly-v1.1.jar ukpmc.AnnotationFilter -stdpipe" #| // #> file("corpora/PMC4969258_PMC4986126.ann") !
  "java -cp lib/monq-1.7.1.jar monq.programs.Grep -r '<z:acc[^>]+>' '</z:acc>' -cr -co -rf '%0<xtext>' '</xtext>%0'" #> file("corpora/PMC4969258_PMC4986126.ann") !
}
// "java -cp target/scala-2.10/AnnotationFilter-assembly-v1.1.jar ukpmc.Pipeline -xml2summary" #> file("corpora/PMC4969258_PMC4986126.ann") !


lazy val testERC = taskKey[Unit]("Prints 'ERC test results'")
testERC := {
  "cat test/ercfunds.txt" #|
	"java -cp lib/monq-1.7.1.jar monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 automata/grants150714.mwt" #|
	"java -cp target/scala-2.10/AnnotationFilter-assembly-v1.1.jar ukpmc.AnnotationFilter -stdpipe" !
}

lazy val testAcc = taskKey[Unit]("Prints 'Acc test results'")
testAcc := {
  "cat test/accnums.txt" #|
	"java -cp lib/monq-1.7.1.jar monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 automata/acc150612.mwt" #|
	"java -cp target/scala-2.10/AnnotationFilter-assembly-v1.1.jar ukpmc.AnnotationFilter -stdpipe" !
}

lazy val testResource = taskKey[Unit]("Prints 'Resource test results'")
testResource := {
	"cat test/accnums.txt" #|
	"java -cp lib/monq-1.7.1.jar monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 automata/resources170405.mwt" #|
	"java -cp target/scala-2.10/AnnotationFilter-assembly-v1.1.jar ukpmc.AnnotationFilter -stdpipe" !
}


lazy val generateEFO = taskKey[Unit]("Generate EFO dictionary")
generateEFO := {
	"rdfparse http://www.ebi.ac.uk/efo/efo.owl" #> file("/tmp/xxxyyyzzz.ttl") #&&
	"arq --data=/tmp/xxxyyyzzz.ttl --query=sparql/efoDPh.rq --results=TSV" #|
	"bin/efoDPh.rb 2" #>
	file("automata/efoDPh.mwt") !
}

lazy val generateChEBI = taskKey[Unit]("Generate ChEBI dictionary")
generateChEBI := {
  // ChEBI need a big memory for arq
	"rdfparse /home/jee/Downloads/chebi.owl" #>
	file("/tmp/xxxyyyzzz.ttl") #&&
	"arq --data=/tmp/xxxyyyzzz.ttl --query=sparql/chebi.rq --results=TSV" #|
	"bin/chebi.rb" #>
	file("automata/chebi.mwt") !
}

lazy val generateGO = taskKey[Unit]("Generate GO dictionary")
generateGO := {}

lazy val generateDOID = taskKey[Unit]("Generate DOID dictionary")
generateDOID := {
	"rdfparse http://www.ebi.ac.uk/ols/beta/ontologies/doid/download" #>
		file("/tmp/xxxyyyzzz.ttl") #&&
		"arq --data=/tmp/xxxyyyzzz.ttl --query=sparql/doid.rq --results=TSV" #|
		"bin/doid.rb 2" #>
		file("automata/doid.mwt") !
}

lazy val generateORDO = taskKey[Unit]("Generate ORDO dictionary")
generateORDO := {
	"rdfparse http://www.ebi.ac.uk/ols/beta/ontologies/ordo/download" #>
		file("/tmp/xxxyyyzzz.ttl") #&&
		"arq --data=/tmp/xxxyyyzzz.ttl --query=sparql/ordo.rq --results=TSV" #|
		"bin/ordo.rb 2" #>
		file("automata/ordo.mwt") !
}

lazy val generateHP = taskKey[Unit]("Generate HP dictionary")
generateHP := {
	"rdfparse http://www.ebi.ac.uk/ols/beta/ontologies/hp/download" #>
		file("/tmp/xxxyyyzzz.ttl") #&&
		"arq --data=/tmp/xxxyyyzzz.ttl --query=sparql/hp.rq --results=TSV" #|
		"bin/hp.rb 2" #>
		file("automata/hp.mwt") !
}

lazy val generateMP = taskKey[Unit]("Generate MP dictionary")
generateMP := {
	"rdfparse http://www.ebi.ac.uk/ols/beta/ontologies/mp/download" #>
		file("/tmp/xxxyyyzzz.ttl") #&&
		"arq --data=/tmp/xxxyyyzzz.ttl --query=sparql/mp.rq --results=TSV" #|
		"bin/mp.rb 2" #>
		file("automata/mp.mwt") !
}

lazy val deploy = taskKey[Unit]("Copies assembly jar to remote location")
deploy := {
  val account = sys.env.get("ACCOUNT").getOrElse("")
	val dpath = sys.env.get("DPATH").getOrElse("")
	val local = assembly.value.getPath
	val remote = account + ":" + dpath + assembly.value.getName
	println(s"Copying: $local -> $remote")
	Seq("scp", local, remote) !
}

// annotate (usage: program ext from to mode date test_date)
lazy val annotate = inputKey[Unit]("Annotation task.")
annotate := {
	val account = sys.env.get("ACCOUNT").getOrElse("")
	val args: Seq[String] = spaceDelimited("<arg>").parsed
	val Seq(date, test_date) = args
	val wd = "/nfs/research2/textmining/jeehyub/pmc/"
	val toDir = wd + date + "/xml/annotation" + test_date
	val to = "annotation" + test_date
	val run = toDir + "/run.sh"
	s"ssh $account cd ~; rm -rf $toDir; mkdir $toDir; run_pipeline xml 170725sec $to annotation $date $test_date > $run" !
	// s"ssh $account cd ~; rm -rf $toDir; mkdir $toDir; run_pipeline xml 170725sec $to annotation $date $test_date | head -1 | sh" !
	// s"ssh $account cd ~; rm -rf $toDir; mkdir $toDir; run_pipeline xml source $to annotation $date $test_date | head -1 | sh" !
}
