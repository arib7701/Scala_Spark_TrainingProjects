package com.ribot.scala.commands

import com.ribot.scala.filesystem.State

trait Command {

  def apply(state: State): State

}

object Command {

  val MKDIR = "mkdir"
  val LS = "ls"
  val PWD = "pwd"
  val TOUCH = "touch"
  val CD = "cd"
  val RM = "rm"
  val ECHO = "echo"
  val CAT = "cat"

  def emptyCommand: Command = new Command {
    override def apply(state: State): State = state
  }

  def incompleteCommand(name: String): Command = new Command {
    override def apply(state: State): State =
      state.setMessage(name + ": incomplete command!")
  }

  def from(input: String): Command = {

    // Split between command and arguments
    val tokens: Array[String] = input.split(" ")

    if (input.isEmpty || tokens.isEmpty) emptyCommand
    else {
      tokens(0) match {
        case LS => new Ls
        case PWD => new Pwd
        case MKDIR if tokens.length < 2 => incompleteCommand(MKDIR)
        case MKDIR => new Mkdir(tokens(1))
        case TOUCH if tokens.length < 2 => incompleteCommand(TOUCH)
        case TOUCH => new Touch(tokens(1))
        case CD if tokens.length < 2 => incompleteCommand(CD)
        case CD => new Cd(tokens(1))
        case RM if tokens.length < 2 => incompleteCommand(RM)
        case RM => new Rm(tokens(1))
        case ECHO if tokens.length < 2 => incompleteCommand(ECHO)
        case ECHO => new Echo(tokens.tail)
        case CAT if tokens.length < 2 => incompleteCommand(CAT)
        case CAT => new Cat(tokens(1))
        case _ => new UnknownCommand
      }
    }
  }
}
