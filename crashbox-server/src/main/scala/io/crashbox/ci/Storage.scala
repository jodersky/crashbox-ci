package io.crashbox.ci

import java.io.{
  BufferedInputStream,
  File,
  FileInputStream,
  FileOutputStream,
  InputStream,
  OutputStream
}
import scala.concurrent.Future
import java.util.UUID
import slick.jdbc.H2Profile.api._

class Storage(implicit core: Core) {
  import core._
  import Scheduler._

  case class Build(
    id: BuildId,
    url: String,
    rebuild: Int,
    state: Int
  )

  class Builds(tag: Tag) extends Table[Build](tag, "builds") {
    def id = column[BuildId]("id", O.PrimaryKey)
    def url = column[String]("url")
    def rebuild = column[Int]("rebuild")
    def state = column[Int]("state")
    def * = (id, url, rebuild, state) <> (Build.tupled, Build.unapply)
  }
  val builds = TableQuery[Builds]

  val database = Database.forConfig("crashbox.db")

  def setupDatabase(): Future[Unit] = {
    log.info("Preparing build database")
    val setup = DBIO.seq(
      builds.schema.create
    )
    database.run(setup)
  }

  def newBuildId(): BuildId = UUID.randomUUID()

  def nextBuild(url: String): Future[Build] = database.run{
    builds.filter(_.url === url).map(_.rebuild).take(1).result.headOption
  }.map{ no =>
    Build(newBuildId(), url, no.getOrElse(0), 0)
  }

  def updateBuildState(buildId: BuildId, state: BuildState) = {
    log.info(s"Build $buildId: state update $state")
  }


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

}
