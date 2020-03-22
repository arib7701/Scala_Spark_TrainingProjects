package com.ribot.scala.files

import com.ribot.scala.filesystem.FilesystemException

class File(override val parentPath: String, override val name: String, val contents: String) extends DirEntry(parentPath, name) {
  override def asDirectory: Directory =
    throw new FilesystemException("File cannot be converted to a Directory")

  override def asFile: File = this

  override def getType: String = "File"

  override def isDirectory: Boolean = false

  override def isFile: Boolean = true

  def setContents(newContents: String) : File =
    new File(parentPath, name, newContents)

  def appendContents(newContents: String) : File =
    new File(parentPath, name, contents + "\n" + newContents)
}

object File {

  def empty(parentPath: String, name: String) : File =
    new File(parentPath, name, "")
}
