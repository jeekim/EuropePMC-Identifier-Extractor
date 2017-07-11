import AssemblyKeys._
// import sbt.complete.DefaultParsers._

assemblySettings

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"
libraryDependencies += "org.specs2" %% "specs2-core" % "3.6.6" % "test"
libraryDependencies += "org.specs2" %% "specs2-gwt" % "3.6.6" % "test"
libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.12.5" % "test"

scalacOptions in (Compile,doc) := Seq("-groups", "-implicits")
scalacOptions in Test ++= Seq("-Yrangepos")

name := "AnnotationFilter"
version := "0.2"

lazy val testERC = taskKey[Unit]("Prints 'ERC test results'")
testERC := {
  "cat test/ercfunds.txt" #|
	"java -cp lib/monq-1.7.1.jar monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 automata/grants150714.mwt" #|
	"java -cp target/scala-2.10/europepmc-identifier-extractor-assembly-0.1-SNAPSHOT.jar ukpmc.AnnotationFilter -stdpipe" !
}

lazy val testAcc = taskKey[Unit]("Prints 'Acc test results'")
testAcc := {
  "cat test/accnums.txt" #|
	"java -cp lib/monq-1.7.1.jar monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 automata/acc150612.mwt" #|
	"java -cp target/scala-2.10/europepmc-identifier-extractor-assembly-0.1-SNAPSHOT.jar ukpmc.AnnotationFilter -stdpipe" !
}

lazy val testResource = taskKey[Unit]("Prints 'Resource test results'")
testResource := {
	"cat test/accnums.txt" #|
	"java -cp lib/monq-1.7.1.jar monq.programs.DictFilter -t elem -e plain -ie UTF-8 -oe UTF-8 automata/resources170405.mwt" #|
	"java -cp target/scala-2.10/europepmc-identifier-extractor-assembly-0.1-SNAPSHOT.jar ukpmc.AnnotationFilter -stdpipe" !
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

val deploy = TaskKey[Unit]("deploy", "Copies assembly jar to remote location")

deploy <<= assembly map { (asm) =>
  val account = sys.env.get("ACCOUNT").getOrElse("")
	val dpath = sys.env.get("DPATH").getOrElse("")
	val local = asm.getPath
	val remote = account + ":" + dpath + asm.getName
	println(s"Copying: $local -> $account:$remote")
	Seq("scp", local, remote) !!
}
