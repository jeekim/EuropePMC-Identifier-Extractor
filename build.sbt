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

lazy val testXXX = taskKey[Unit]("Prints 'Hello World'")

// testXXX := {
//  "cat test/xxx.txt" #| "java -cp lib/monq-1.7.1.jar monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 automata/xxx.mwt" !
//}

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
  val account = "jhkim@ebi-001.ebi.ac.uk"
  val wd = "/nfs/research2/textmining/jeehyub/pmc/"
  val toDir = wd + date + "/xml/annotation" + test_date
  val to = "annotation" + test_date
  val run = toDir + "/run.sh"
  s"ssh $account cd ~; rm -rf $toDir; mkdir $toDir; run_pipeline xml source $to annotation $date $test_date | head -1 | sh" !
}

// prepareWorkingDirectory

// runPipeline
