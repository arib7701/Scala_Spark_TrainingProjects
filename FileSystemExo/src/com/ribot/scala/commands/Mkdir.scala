package com.ribot.scala.commands

import com.ribot.scala.files.{DirEntry, Directory}
import com.ribot.scala.filesystem.State

class Mkdir(name: String) extends CreateEntry(name) {

  override def createSpecificEntry(state: State): DirEntry =
    Directory.empty(state.wd.path, name)
}
