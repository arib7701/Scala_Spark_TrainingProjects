package com.ribot.scala.commands

import com.ribot.scala.files.{DirEntry, File}
import com.ribot.scala.filesystem.State

class Touch(name: String) extends CreateEntry(name) {
  override def createSpecificEntry(state: State): DirEntry =
    File.empty(state.wd.path, name)
}
