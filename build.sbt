import AssemblyKeys._
import sbt.complete.DefaultParsers._

assemblySettings

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

libraryDependencies += "org.specs2" %% "specs2-core" % "3.6.6" % "test"

libraryDependencies += "org.specs2" %% "specs2-gwt" % "3.6.6" % "test"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.12.5" % "test"

scalacOptions in (Compile,doc) := Seq("-groups", "-implicits")

scalacOptions in Test ++= Seq("-Yrangepos")

lazy val testERC = taskKey[Unit]("Prints 'Hello World'")

testERC := {
  "cat test/ercfunds.txt" #| "java -cp lib/monq-1.7.1.jar monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 automata/grants150714.mwt" #| "java -cp target/scala-2.10/europepmc-identifier-extractor-assembly-0.1-SNAPSHOT.jar ukpmc.ValidateAccessionNumber -stdpipe" !
}

lazy val testAcc = taskKey[Unit]("Prints 'Hello World'")

testAcc := {
  "cat test/accnums.txt" #| "java -cp lib/monq-1.7.1.jar monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 automata/acc150612.mwt" #| "java -cp target/scala-2.10/europepmc-identifier-extractor-assembly-0.1-SNAPSHOT.jar ukpmc.ValidateAccessionNumber -stdpipe" !
}

// lazy val testXXX = taskKey[Unit]("Prints 'Hello World'")

// testXXX := {
//  "cat test/xxx.txt" #| "java -cp lib/monq-1.7.1.jar monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 automata/xxx.mwt" !
//}

lazy val generateEFO = taskKey[Unit]("Generate EFO dictionary")

generateEFO := {
	"rdfparse http://www.ebi.ac.uk/efo/efo.owl" #> file("/tmp/xxxyyyzzz.ttl") #&& "arq --data=/tmp/xxxyyyzzz.ttl --query=sparql/efoDPh.rq --results=TSV" #| "bin/efoDPh.rb 2" #> file("automata/efoDPh.mwt") !
}

lazy val generateDOID = taskKey[Unit]("Generate DOID dictionary")

generateDOID := {
	"rdfparse http://www.ebi.ac.uk/ols/beta/ontologies/doid/download" #> file("/tmp/xxxyyyzzz.ttl") #&& "arq --data=/tmp/xxxyyyzzz.ttl --query=sparql/doid.rq --results=TSV" #| "bin/doid.rb 2" #> file("automata/doid.mwt") !
}

val deployTask = TaskKey[Unit]("deploy", "Copies assembly jar to remote location")

deployTask <<= assembly map { (asm) =>
  val account = "jhkim@ebi-001.ebi.ac.uk"
  val local = asm.getPath
  val remote = account + ":" + "/nfs/misc/literature/textmining/ePMC/lib/" + asm.getName
  println(s"Copying: $local -> $account:$remote")
  Seq("scp", local, remote) !!
}

// deployDictionary

// buildPipeline

// annotate (usage: program ext from to mode date test_date)
val annotate = inputKey[Unit]("Annotation task.")

annotate := {
  val args: Seq[String] = spaceDelimited("<arg>").parsed
  // val Seq(ext, from, to, mode, date, test_date) = args
  val Seq(date, test_date) = args
  val account = ""
  val wd = "/nfs/research2/textmining/jeehyub/pmc/"
  val toDir = wd + date + "/xml/annotation" + test_date
  val to = "annotation" + test_date
  val run = toDir + "/run.sh"
  s"ssh $account cd ~; rm -rf $toDir; mkdir $toDir; run_pipeline xml source $to annotation $date $test_date | head -1 | sh" !
}

val rdf = inputKey[Unit]("4rdf task.")

rdf := {
  val args: Seq[String] = spaceDelimited("<arg>").parsed
  // val Seq(ext, from, to, mode, date, test_date) = args
  val Seq(date, test_date) = args
  val account = ""
  val wd = "/nfs/research2/textmining/jeehyub/pmc/"
  val fromDir = wd + date + "/xml/annotation" + test_date
  val toDir = wd + date + "/xml/4rdf" + test_date
  val from = "annotation" + test_date
  val to = "4rdf" + test_date
  val run = toDir + "/run.sh"
  s"ssh $account cd ~; rm -rf $toDir; mkdir $toDir; run_pipeline xml $from $to 4rdf $date $test_date | head -1 | sh" !
}

// prepareWorkingDirectory

// runPipeline
