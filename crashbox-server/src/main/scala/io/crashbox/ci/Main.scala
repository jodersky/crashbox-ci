package io.crashbox.ci

import scala.util.{Failure, Success}

import akka.http.scaladsl.Http
import scala.concurrent._
import scala.concurrent.duration._

object Main
    extends Core
    with Schedulers
    with Storage
    with Executors
    with Parsers
    with Source
    with HttpApi {

  def main(args: Array[String]): Unit = {
    reapDeadBuilds()
    Await.result(setupDatabase(), 10.seconds)

    val host = config.getString("crashbox.host")
    val port = config.getInt("crashbox.port")
    Http(system).bindAndHandle(httpApi, host, port) onComplete {
      case Success(_) =>
        log.info(s"Listening on $host:$port")
      case Failure(ex) =>
        log.error(ex, s"Failed to bind to $host:$port")
        system.terminate()
    }
  }

}
