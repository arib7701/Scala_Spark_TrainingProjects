package com.ribot.scala.commands

import com.ribot.scala.filesystem.State

class Pwd extends Command {
  override def apply(state: State): State =
    state.setMessage(state.wd.path)
}
