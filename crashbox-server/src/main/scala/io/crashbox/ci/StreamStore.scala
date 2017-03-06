package io.crashbox.ci

import java.io.{
  File,
  FileInputStream,
  FileOutputStream,
  InputStream,
  OutputStream
}
import java.security.MessageDigest

trait StreamStore { self: Core =>

  val streamsDirectory: File = new File(
    config.getString("crashbox.streams.directory"))

  private def logFile(id: String): File = {
    val bytes = MessageDigest.getInstance("SHA-256").digest(id.getBytes)
    val str = bytes.map { byte =>
      Integer.toString((byte & 0xff) + 0x100, 16)
    }.mkString
    val (head, tail) = str.splitAt(2)
    new File(streamsDirectory, s"$head/$tail")
  }

  def saveStream(id: String): OutputStream = {
    val file = logFile(id)
    file.getParentFile.mkdirs()
    file.createNewFile()
    file.setWritable(true)
    new FileOutputStream(file)
  }

  def readStream(id: String): InputStream = {
    new FileInputStream(logFile(id))
  }

}
