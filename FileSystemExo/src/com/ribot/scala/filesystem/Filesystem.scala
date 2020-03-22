package com.ribot.scala.filesystem

import java.util.Scanner

import com.ribot.scala.commands.Command
import com.ribot.scala.files.Directory

object Filesystem extends App{

  val root = Directory.ROOT
  val scanner = new Scanner(System.in)

  // mutable - to be able to update state
  var state = State(root, root)

  while(true) {
    state.show
    val input = scanner.nextLine()

    // Create new Command with user input and apply that command to old state to obtain a new state
    state = Command.from(input).apply(state)
  }
}
