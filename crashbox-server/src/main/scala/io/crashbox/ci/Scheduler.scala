package io.crashbox.ci

import java.io.{File, OutputStream}
import java.net.URL
import java.nio.file.Files

import scala.collection.mutable.HashMap
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.util.Timeout

class Scheduler(
  val executor: DockerExecutor,
  storage: Storage
)(implicit core: Core) {
  import Scheduler._
  import core._
  import executor._
  import storage._

  private def newTempDir: File =
    Files.createTempDirectory("crashbox-run").toFile()

  class BuildManager(
      buildId: BuildId,
      url: URL
  ) extends Actor
      with ActorLogging {

    var buildDir: Option[File] = None
    var out: Option[OutputStream] = None
    var containerId: Option[ExecutionId] = None

    override def postStop() = {
      containerId foreach { cancelExecution(_) }
      out foreach { _.close() }
      buildDir foreach { _.delete() }
      log.info(s"Stopped build of $url")
    }

    override def preStart() = {
      log.info(s"Started build of $url")
      self ! Cloning(url)
    }

    override def receive: Receive = {

      case state @ Cloning(url) =>
        log.debug("Update build state: cloning")
        updateBuildState(buildId, state)
        Git.fetchSource(url, newTempDir) onComplete {
          case Success(dir) =>
            self ! Parsing(dir)
          case Failure(err) =>
            self ! Failed(s"Error fetching source from $url")
        }

      case state @ Parsing(src) =>
        log.debug("Update build state: parsing")
        updateBuildState(buildId, state)
        buildDir = Some(src)
        Parser.parseBuild(src) match {
          case Left(buildDef) =>
            self ! Starting(src, buildDef)
          case Right(err) =>
            self ! Failed(s"Failed to parse build $err")
        }

      case state @ Starting(src, bd) =>
        log.debug("Update build state: starting")
        updateBuildState(buildId, state)
        val so = saveLog(buildId, 0)
        out = Some(so)
        startExecution(bd.image, bd.script, src, so) onComplete {
          case Success(id) =>
            self ! Running(id)
          case Failure(err) =>
            self ! Failed(s"Failed to start build $err")
        }

      case state @ Running(id) =>
        log.debug("Update build state: running")
        updateBuildState(buildId, state)
        containerId = Some(id)
        waitExecution(id) onComplete {
          case Success(status) =>
            self ! Finished(status)
          case Failure(err) =>
            self ! Failed(s"Error waiting for build to complete")
        }

      case state @ Finished(status) =>
        log.debug("Update build state: finished")
        updateBuildState(buildId, state)
        context stop self

      case state @ Failed(message) =>
        log.debug("Update build state: failed")
        updateBuildState(buildId, state)
        context stop self
    }
  }
  object BuildManager {
    def apply(buildId: BuildId, url: URL) =
      Props(new BuildManager(buildId, url))
  }

  private sealed trait SchedulerCommand
  private case class ScheduleBuild(url: URL) extends SchedulerCommand
  private case class CancelBuild(buildId: BuildId) extends SchedulerCommand

  class Scheduler extends Actor {
    val runningBuilds = new HashMap[BuildId, ActorRef]

    override def receive = {

      case ScheduleBuild(url) =>
        val client = sender
        //todo handle failure
        nextBuild(url.toString).foreach{ build =>
          val buildManager =
            context.actorOf(BuildManager(build.id, url), s"build-${build.id}")
          context watch buildManager
          runningBuilds += build.id -> buildManager
          client ! build.id
        }

      case CancelBuild(id) =>
        runningBuilds.get(id).foreach { builder =>
          context.stop(builder)
        }

      case Terminated(buildManager) =>
        //TODO use a more efficient data structure
        runningBuilds.find(_._2 == buildManager).foreach {
          runningBuilds -= _._1
        }
    }
  }

  private val scheduler =
    system.actorOf(Props(new Scheduler()), "crashbox-scheduler")

  // None if build can not be scheduled (queue is full)
  def scheduleBuild(url: URL): Future[BuildId] = {
    import akka.pattern.ask
    implicit val timeout: Timeout = Timeout(5.seconds)
    (scheduler ? ScheduleBuild(url)).mapTo[BuildId]
  }

  def cancelBuild(buildId: BuildId): Unit = {
    scheduler ! CancelBuild(buildId)
  }

}

object Scheduler {

  sealed trait BuildState
  case class Cloning(url: URL) extends BuildState
  case class Parsing(dir: File) extends BuildState
  case class Starting(dir: File, buildDef: BuildDef) extends BuildState
  case class Running(id: ExecutionId) extends BuildState

  sealed trait EndBuildState extends BuildState
  case class Finished(status: Int) extends EndBuildState
  case class Failed(message: String) extends EndBuildState

}
