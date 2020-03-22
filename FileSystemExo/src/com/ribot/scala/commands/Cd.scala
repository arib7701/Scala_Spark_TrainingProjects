package com.ribot.scala.commands

import com.ribot.scala.files.{DirEntry, Directory}
import com.ribot.scala.filesystem.State

import scala.annotation.tailrec

class Cd(dir: String) extends Command {

  override def apply(state: State): State = {

    // find root
    val root = state.root
    val wd = state.wd

    // find absolute path of the directory to cd to
    val absolutePath =
      if(dir.startsWith(Directory.SEPARATOR)) dir
      else if(wd.isRoot) wd.path + dir
      else wd.path + Directory.SEPARATOR + dir

    // find the directory to cd to given the path
    val destinationDirectory = doFindEntry(root, absolutePath)

    // change the state given the new directory
    if(destinationDirectory == null || !destinationDirectory.isDirectory)
      state.setMessage(dir + " : no such directory")
    else
      State(root, destinationDirectory.asDirectory)
  }

  def doFindEntry(root: Directory, path: String): DirEntry = {

    @tailrec
    def findEntryHelper(currentDir: Directory, path: List[String]): DirEntry = {
      if(path.isEmpty || path.head.isEmpty) currentDir
      else if(path.tail.isEmpty) currentDir.findEntry(path.head)
      else {
        val nextDir = currentDir.findEntry(path.head)
        if(nextDir == null || !nextDir.isDirectory) null
        else findEntryHelper(nextDir.asDirectory, path.tail)
      }
    }

    @tailrec
    def collapseRelativeTokens(path: List[String], result: List[String]): List[String] = {
      if(path.isEmpty) result
      else if(".".equals(path.head)) collapseRelativeTokens(path.tail, result)
      else if("..".equals(path.head)) {
        if(result.isEmpty) null
        else collapseRelativeTokens(path.tail, result.init)
      } else collapseRelativeTokens(path.tail, result :+ path.head)
    }

    // tokens
    val tokens: List[String] = path.substring(1).split(Directory.SEPARATOR).toList

    // eliminate/collapse relative tokens: . and ..
    val newTokens = collapseRelativeTokens(tokens, List())
    if(newTokens == null) null

    // navigate to the correct entry
    findEntryHelper(root, newTokens)
  }
}

/* example

/a/. => ["a", "."] => ["a"]
/a/b/. => ["a", "b", "."] => ["a", "b"]
/a/.. => ["a", ".."] => []
/a/b/.. => ["a", "b", ".."] => ["a"]

 */
