package io.crashbox.ci

import akka.actor.{ Actor, ActorLogging, ActorRef, OneForOneStrategy, Props, Terminated }
import akka.stream.stage.GraphStageLogic
import akka.stream.{ Attributes, Outlet, SourceShape }
import akka.stream.stage.GraphStage
import java.io.{ File, OutputStream }
import java.net.URL
import java.nio.file.Files
import java.util.Base64
import scala.collection.mutable.HashMap
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.control.NonFatal
import akka.actor.SupervisorStrategy._
import scala.util.{ Failure, Success }


trait Schedulers extends { self: Core with Source with Builders with Parsers =>

  private def newTempDir: File = Files.createTempDirectory("crashbox-run").toFile()

  sealed trait BuildState
  case class Cloning(url: URL) extends BuildState
  case class Parsing(dir: File) extends BuildState
  case class Starting(dir: File, buildDef: BuildDef) extends BuildState
  case class Running(id: ContainerId) extends BuildState

  sealed trait EndBuildState extends BuildState
  case class Finished(status: Int) extends EndBuildState
  case class Failed(message: String) extends EndBuildState

  class BuildManager(
    url: URL,
    openOut: () => OutputStream,
    update: BuildState => Unit
  ) extends Actor with ActorLogging {

    var buildDir: Option[File] = None
    var out: Option[OutputStream] = None
    var containerId: Option[ContainerId] = None

    override def postStop() = {
      containerId foreach { cancelBuild(_) }
      out foreach { _.close() }
      buildDir foreach { _.delete() }
    }

    override def preStart() = {
      log.info(s"Started build manager for $url")
      self ! Cloning(url)
    }

    override def receive: Receive = {

      case state@Cloning(url) =>
        log.debug("Update build state: cloning")
        update(state)
        fetchSource(url, newTempDir) onComplete {
          case Success(dir) =>
            self ! Parsing(dir)
          case Failure(err) =>
            self ! Failed(s"Error fetching source from $url")
        }

      case state@Parsing(src) =>
        log.debug("Update build state: parsing")
        update(state)
        buildDir = Some(src)
        parseBuild(src) match {
          case Left(buildDef) =>
            self ! Starting(src, buildDef)
          case Right(err) =>
            self ! Failed(s"Failed to parse build $err")
        }

      case state@Starting(src, bd) =>
        log.debug("Update build state: starting")
        update(state)
        val so = openOut()
        out = Some(so)
        startBuild(bd.image, bd.script, src, so) onComplete {
          case Success(id) =>
            self ! Running(id)
          case Failure(err) =>
            self ! Failed(s"Failed to start build $err")
        }

      case state@Running(id) =>
        log.debug("Update build state: running")
        update(state)
        containerId = Some(id)
        waitBuild(id) onComplete {
          case Success(status) =>
            self ! Finished(status)
          case Failure(err) =>
            self ! Failed(s"Error waiting for build to complete")
        }

      case state@Finished(status) =>
        log.debug("Update build state: finished")
        update(state)
        context stop self

      case state@Failed(message) =>
        log.debug("Update build state: failed")
        update(state)
        context stop self
    }
  }
  object BuildManager {
    def apply(buildId: String, url: URL, out: () => OutputStream, update: BuildState => Unit) =
      Props(new BuildManager(url, out, update))
  }

  private sealed trait SchedulerCommand
  private case class ScheduleBuild(
    buildId: String, url: URL, out: () => OutputStream, update: BuildState => Unit
  ) extends SchedulerCommand

  class Scheduler extends Actor {

    val runningBuilds = new HashMap[String, ActorRef]

    override def receive = {

      case sb: ScheduleBuild =>
        runningBuilds.get(sb.buildId) match {
          case Some(_) => //already running
          case None =>
            val buildManager = context.actorOf(BuildManager(
              sb.buildId, sb.url, sb.out, sb.update), s"build-${sb.buildId}")
            context watch buildManager
            runningBuilds += sb.buildId -> buildManager
        }

      case Terminated(buildManager) =>
        //TODO use a more efficient data structure
        runningBuilds.find(_._2 == buildManager).foreach {
          runningBuilds -= _._1
        }
    }
  }

  private val scheduler = system.actorOf(Props(new Scheduler()), "crashbox-scheduler")

  def start(
    buildId: String,
    url: URL,
    out: () => OutputStream,
    update: BuildState => Unit
  ): Unit = {
    scheduler ! ScheduleBuild(buildId, url, out, update)
  }

}
