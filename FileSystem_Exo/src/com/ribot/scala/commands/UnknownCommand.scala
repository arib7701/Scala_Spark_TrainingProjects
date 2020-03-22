package com.ribot.scala.commands

import com.ribot.scala.filesystem.State

class UnknownCommand extends Command {

  override def apply(state: State): State =
    state.setMessage("Command not found!")

}
