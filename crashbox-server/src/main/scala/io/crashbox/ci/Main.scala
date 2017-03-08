package io.crashbox.ci

import scala.util.{Failure, Success}

import akka.http.scaladsl.Http

object Main
    extends Core
    with Schedulers
    with Builders
    with Parsers
    with Source
    with StreamStore
    with HttpApi {

  def main(args: Array[String]): Unit = {
    reapDeadBuilds()

    val host = system.settings.config.getString("crashbox.host")
    val port = system.settings.config.getInt("crashbox.port")

    Http(system).bindAndHandle(httpApi, host, port) onComplete {
      case Success(_) =>
        system.log.info(s"Listening on $host:$port")
      case Failure(ex) =>
        system.log.error(ex, s"Failed to bind to $host:$port")
        system.terminate()
    }
  }

}
