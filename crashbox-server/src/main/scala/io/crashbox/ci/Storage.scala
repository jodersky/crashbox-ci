package io.crashbox.ci

import java.io.{
  BufferedInputStream,
  File,
  FileInputStream,
  FileOutputStream,
  InputStream,
  OutputStream
}
import java.util.UUID

import slick.driver.H2Driver.api._

trait Storage { self: Core with Parsers with Schedulers =>

  type BuildId = UUID


  
  def newBuildId() = UUID.randomUUID()



  private val streamsDirectory: File = new File(
    config.getString("crashbox.streams.directory"))

  private def logFile(buildId: BuildId, task: Int): File = {
    def stringifyId(id: BuildId): String = {
      val bytes = new Array[Byte](16) // 128 bits
      for (i <- 0 until 8) {
        bytes(i) = ((id.getLeastSignificantBits >> i) & 0xff).toByte
      }
      for (i <- 0 until 8) {
        bytes(8 + i) = ((id.getMostSignificantBits >> i) & 0xff).toByte
      }
      bytes.map { byte =>
        Integer.toString((byte & 0xff) + 0x100, 16)
      }.mkString
    }
    val (dir1, tail) = stringifyId(buildId).splitAt(2)
    val (dir2, dir3) = tail.splitAt(2)
    new File(streamsDirectory, s"$dir1/$dir2/$dir3/$task")
  }

  def saveLog(buildId: BuildId, task: Int): OutputStream = {
    val file = logFile(buildId, task)
    file.getParentFile.mkdirs()
    file.createNewFile()
    file.setWritable(true)
    new FileOutputStream(file)
  }

  def readLog(buildId: BuildId, task: Int): InputStream = {
    new FileInputStream(logFile(buildId, task))
  }

  def updateBuildState(buildId: BuildId, state: BuildState) = {
    log.info(s"Build $buildId: state update $state")
  }

}
