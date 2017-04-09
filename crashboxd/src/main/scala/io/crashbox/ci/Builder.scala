package io.crashbox.ci

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem }
import akka.stream.stage.{ GraphStageLogic, InHandler, OutHandler, StageLogging }
import akka.stream.{ Attributes, FanInShape2, Inlet, Outlet }
import akka.stream.stage.{ GraphStage }
import io.crashbox.ci.DockerExecutor.ExecutionId
import java.io.{ File, OutputStream }
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.concurrent.TrieMap
import scala.collection.mutable.{ HashMap, Queue }
import scala.concurrent.Future
import scala.util.{ Failure, Success }


case class BuildId(id: String) extends AnyVal

case class TaskId(buildId: String, taskIdx: Int) {
  override def toString = s"$buildId#$taskIdx"
}

class BuildStage(
  executor: DockerExecutor,
  mkdir: TaskId => File,
  mkout: TaskId => OutputStream
) extends GraphStage[FanInShape2[(TaskId, TaskDef), TaskId, Builder.BuildState]] {
  import Builder._

  val submissions = Inlet[(TaskId, TaskDef)]("Builder.submissions")
  val cancellations = Inlet[TaskId]("Builder.cancellations")
  val states = Outlet[BuildState]("Builder.states")

  val shape = new FanInShape2(submissions, cancellations, states)

  def createLogic(attributes: Attributes) = new GraphStageLogic(shape) with StageLogging {
    implicit def ec = materializer.executionContext
    //import scala.concurrent.ExecutionContext.Implicits.global

    var runningTaskId: Option[TaskId] = None
    var runningExecutionId: Future[ExecutionId] =
      Future.failed(new RuntimeException("not started"))

    override def preStart(): Unit = {
      log.info("Starting build stage")
      pull(cancellations)
      pull(submissions)
    }

    setHandler(cancellations, new InHandler {

      override def onPush(): Unit = {
        val tid = grab(cancellations)
        if (runningTaskId == tid) {
          runningExecutionId.foreach { eid =>
            executor.stop(eid)
          }
        }
        pull(cancellations)
      }

      // don't finish stage when cancellations closes
      override def onUpstreamFinish() = {}

    })

    setHandler(submissions, new InHandler {

      val changeState = getAsyncCallback[BuildState]{state =>
        log.info(s"${state.taskId} transitioned to $state")
        emit(states, state)
      }
      val pullNext = getAsyncCallback[Unit]{ _ =>
        if (isAvailable(states)) {
          pull(submissions)
        }
      }

      override def onPush(): Unit = {
        val (tid, tdef) = grab(submissions)
        log.info(s"$tid new submission $tdef")

        val image = tdef.environment match {
          case DockerEnvironment(image) => image
          case env => sys.error("Unsupported environemnt")
        }
        
        changeState.invoke(TaskStarting(tid, tdef))
        runningTaskId = Some(tid)
        runningExecutionId = executor.start(
          image,
          tdef.script,
          mkdir(tid),
          mkout(tid)
        ).map{ eid =>
          changeState.invoke(TaskRunning(tid, eid))
          eid
        }
        
        runningExecutionId.flatMap { eid =>
          executor.result(eid)
        } onComplete { result =>
          result match {
            case Success(status) =>
              changeState.invoke(TaskFinished(tid, status))
            case Failure(err) =>
              changeState.invoke(TaskFailed(tid, err.toString))
          }
          pullNext.invoke(())
        }
      }

      override def onUpstreamFinish() = {
        log.info(s"${submissions} finished, completing stage after final build")
        val callback = getAsyncCallback[Unit]{_ =>
          completeStage()
        }
        runningExecutionId.onComplete { _ => callback.invoke(()) }
      }

    })

    setHandler(states, GraphStageLogic.IgnoreTerminateOutput)

    override def postStop() = {
      log.info("postStop")
      runningExecutionId foreach { eid =>
        log.error("kill")
        executor.stop(eid)
      }
    }

  }
}

// class Builder(
//   listener: ActorRef,
//   exec: DockerExecutor,
//   mkdir: (BuildId, Int) => File,
//   mkout: (BuildId, Int) => OutputStream
// ) extends Actor with ActorLogging {
//   import context._
//   import Builder._

//   val queue = Queue.empty[(BuildId, Int, BuildDef)]
//   val running = TrieMap.empty[(BuildId, Int), ExecutionId]

//   val Parallelism = 2
//   var slots = Parallelism

//   def receive = {

//     case Cancel(bid) =>
//       running.find(_._1._1 == bid).foreach { case (_, eid)  =>
//         // stopping will cause the pipeline to fail an thus remove itself from
//         // the running map
//         exec.stop(eid)
//       }

//     case Done =>
//       slots += 1
//       listener ! Next

//     case Next if slots > 0 =>
//       slots -= 1
//       val (bid, tid, bdf) = queue.dequeue()
//       val tdf = bdf.tasks(tid)
//       val image = tdf.environment match {
//         case DockerEnvironment(image) => image
//         case env => {log.error(s"can't run $env"); ???}
//       }

//       listener ! TaskStarting(bid, tid, tdf)

//       val pipeline = for (
//         eid <- exec.start(image, tdf.script, mkdir(bid, tid), mkout(bid, tid));
//         _ = listener ! TaskRunning(bid, tid, eid);
//         _ = running += (bid, tid) -> eid;
//         status <- exec.result(eid)
//       ) yield {
//         status
//       }
//       pipeline onComplete { res =>
//         running -= ((bid, tid))
//         self ! Done
//         res match {
//           case Success(status) => listener ! TaskFinished(bid, tid, status)
//           case Failure(err) => listener ! TaskFailed(bid, tid, err.toString)
//         }
//       }

//     case Next => // no slots available, do nothing

//   }

//   override def postStop() = {
//     for ((_, eid) <- running) {
//       exec.stop(eid)
//     }
//   }

// }

object Builder {

  //case class Submit(id: BuildId, build: BuildDef)
  //case class Cancel(id: BuildId)

  private case object Next
  private case object Done

  sealed trait BuildState {
    def taskId: TaskId
  }
  case class TaskStarting(taskId: TaskId, taskDef: TaskDef) extends BuildState
  case class TaskRunning(taskId: TaskId, execId: ExecutionId) extends BuildState

  case class TaskFinished(taskId: TaskId, status: Int) extends BuildState
  case class TaskFailed(taskId: TaskId, message: String) extends BuildState

}
