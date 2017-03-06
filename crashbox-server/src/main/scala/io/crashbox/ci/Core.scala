package io.crashbox.ci

import akka.actor.ActorSystem
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

trait Core {

  implicit val system: ActorSystem = ActorSystem("crashbox")
  implicit val executionContext: ExecutionContext = system.dispatcher
  val blockingDispatcher: ExecutionContext =
    system.dispatchers.lookup("crashbox.blocking-dispatcher")

  def log = system.log
  def config = system.settings.config

  sys.addShutdownHook {
    log.info("Shutting down systm")
    Await.ready(system.terminate(), Duration.Inf)
    println("shutdown")
  }

}
