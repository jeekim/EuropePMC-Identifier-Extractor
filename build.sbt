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
