package io.crashbox.ci

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.directives.Credentials
import akka.stream.ActorMaterializer
import java.text.SimpleDateFormat
import java.util.Date
import scala.util.{Failure, Success}

object Main {

  implicit val system = ActorSystem("crashbox")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  def main(args: Array[String]): Unit = {
    val host = system.settings.config.getString("crashbox.host")
    val port = system.settings.config.getInt("crashbox.port")

    Http(system).bindAndHandle(Route.route, host, port) onComplete {
      case Success(_) =>
        system.log.info(s"Listening on $host:$port")
      case Failure(ex) =>
        system.log.error(ex, s"Failed to bind to $host:$port")
        system.terminate()
    }
  }

}

object Route {
  import akka.http.scaladsl.server._
  import Directives._

  private val formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  def route: Route = {
    pathEndOrSingleSlash {
      complete(s"Served at ${formatter.format(new Date)}")
    } ~ path("secure") {
      authenticateBasic(realm = "secure site", myUserPassAuthenticator) {
        userName =>
          complete(s"The user is '$userName'")
      }
    }
  }

  def myUserPassAuthenticator(credentials: Credentials): Option[String] = {
    credentials match {
      case p @ Credentials.Provided(id) if p.verify("guest") => Some(id)
      case _ => None
    }
  }
}
