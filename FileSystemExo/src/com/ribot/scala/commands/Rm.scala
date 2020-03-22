package com.ribot.scala.commands

import com.ribot.scala.files.Directory
import com.ribot.scala.filesystem.State

import scala.annotation.tailrec

class Rm(name: String) extends Command {

  override def apply(state: State): State = {

    // get working directory
    val wd = state.wd

    // get absolute path
    val absolutePath =
      if(name.startsWith(Directory.SEPARATOR)) name
      else if(wd.isRoot) wd.path + name
      else wd.path + Directory.SEPARATOR + name

    // checks validation
    if(Directory.ROOT_PATH.equals(absolutePath))
      state.setMessage("Not supported")
    else
      doRm(state, absolutePath)
  }

  def doRm(state: State, path: String): State = {

    def doRmHelper(currentDirectory: Directory, path: List[String]): Directory = {
      if(path.isEmpty) currentDirectory
      else if(path.tail.isEmpty) currentDirectory.removeEntry(path.head)
      else {
        val nextDirectory = currentDirectory.findEntry(path.head)
        if(!nextDirectory.isDirectory) currentDirectory
        else {
          val newNextDirectory = doRmHelper(nextDirectory.asDirectory, path.tail)
          if(newNextDirectory == nextDirectory) currentDirectory
          else currentDirectory.replaceEntry(path.head, newNextDirectory)
        }
      }
    }

    // find the entry to remove & update structure
    val tokens = path.substring(1).split(Directory.SEPARATOR).toList
    val newRoot : Directory = doRmHelper(state.root, tokens)

    if(newRoot == state.root)
      state.setMessage(path + " no such file or directory")
    else
      State(newRoot, newRoot.findDescendant(state.wd.path.substring(1)))
  }
}
