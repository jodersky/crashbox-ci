package io.crashbox.ci

import scala.util.{Failure, Success}

import akka.http.scaladsl.Http
import scala.concurrent._
import scala.concurrent.duration._

object Main {

  implicit val core = new Core()
  import core._

  val storage = new Storage()
  val executor = new DockerExecutor
  val scheduler = new Scheduler(executor, storage)
  val api = new HttpApi(scheduler, storage)


  def main(args: Array[String]): Unit = {
    executor.reapDeadBuilds()
    Await.result(storage.setupDatabase(), 10.seconds)

    val host = config.getString("crashbox.host")
    val port = config.getInt("crashbox.port")
    Http(system).bindAndHandle(api.httpApi, host, port) onComplete {
      case Success(_) =>
        log.info(s"Listening on $host:$port")
      case Failure(ex) =>
        log.error(ex, s"Failed to bind to $host:$port")
        system.terminate()
    }
  }

}
