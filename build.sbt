import AssemblyKeys._

assemblySettings

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

libraryDependencies += "org.specs2" %% "specs2-core" % "3.6.6" % "test"

libraryDependencies += "org.specs2" %% "specs2-gwt" % "3.6.6" % "test"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.12.5" % "test"

// libraryDependencies += "com.chuusai" %% "shapeless" % "2.2.5" % "test"

// libraryDependencies += "org.specs2" %% "specs2" % "3.3.1" % "test"

// libraryDependencies += "junit" % "junit" % "4.10" % "test"

// libraryDependencies += "com.novocode" % "junit-interface" % "0.8" % "test"


// libraryDependencies ++= Seq("org.specs2" %% "specs2-core" % "3.6.6" % "test")

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

val deployTask = TaskKey[Unit]("deploy", "Copies assembly jar to remote location")

deployTask <<= assembly map { (asm) =>
  val account = "jhkim@ebi-001.ebi.ac.uk"
  val local = asm.getPath
  val remote = account + ":" + "/nfs/misc/literature/textmining/ePMC/lib/" + asm.getName
  println(s"Copying: $local -> $account:$remote")
  Seq("scp", local, remote) !!
}
