package io.crashbox.ci

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.stream.ActorMaterializer
import com.typesafe.config.Config

class Core {

  implicit val system: ActorSystem = ActorSystem("crashbox")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher
  val blockingDispatcher: ExecutionContext =
    system.dispatchers.lookup("crashbox.blocking-dispatcher")

  def log: LoggingAdapter = system.log
  def config: Config = system.settings.config

  sys.addShutdownHook {
    log.info("Shutting down core system")
    Await.ready(system.terminate(), Duration.Inf)
    log.info("System stopped")
  }

}
