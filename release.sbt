val ReleaseCommand = Command.command("release") {
  state =>
    "test" :: "testERC" :: state
}

def helloAll = Command.args("helloAll", "<name>") { (state, args) =>
  println("Hi " + args.mkString(" "))
    state
}

commands ++= Seq(ReleaseCommand, helloAll)

// commands += ReleaseCommand

// annotate

// summarise
