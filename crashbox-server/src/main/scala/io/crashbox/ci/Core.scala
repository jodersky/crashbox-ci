package io.crashbox.ci

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

trait Core {

  implicit val system: ActorSystem = ActorSystem("crashbox")
  implicit val materializer = ActorMaterializer()
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
