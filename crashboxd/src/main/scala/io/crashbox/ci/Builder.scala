package io.crashbox.ci

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem }
import akka.stream.SourceShape
import akka.stream.stage.{ GraphStageLogic, InHandler, OutHandler, StageLogging }
import akka.stream.{ Attributes, FanInShape2, Inlet, Outlet }
import akka.stream.stage.{ GraphStage }
import java.io.{ File, OutputStream }
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.concurrent.TrieMap
import scala.collection.mutable.{ HashMap, Queue }
import scala.concurrent.Future
import scala.util.{ Failure, Success }


class BuildSource[Env <: Environment, Id <: ExecutionId](
  taskId: TaskId,
  taskDef: TaskDef[Env],
  executor: Executor[Env, Id],
  mkdir: => File,
  mkout: => OutputStream // TODO refactor this into a two-output stage
) extends GraphStage[SourceShape[Builder.BuildState]] {
  import Builder._

  val states = Outlet[BuildState]("Builder.states")

  val shape = new SourceShape(states)

  def createLogic(attributes: Attributes) = new GraphStageLogic(shape) with StageLogging {
    implicit def ec = materializer.executionContext

    lazy val instance: Future[Id] = executor.start(
      taskDef.environment,
      taskDef.script,
      mkdir,
      mkout
    )

    val changeState = getAsyncCallback[BuildState]{state =>
      log.info(s"${state.taskId} transitioned to $state")
      emit(states, state)
    }

    val asyncClose = getAsyncCallback[Unit]{_ =>
      completeStage()
    }

    override def preStart() = {
      val pipeline = for (
        _ <- Future.unit;
        _ = changeState.invoke(TaskStarting(taskId, taskDef));
        eid <- instance;
        _ = changeState.invoke(TaskRunning(taskId, eid));
        status <- executor.result(eid)
      ) yield status

      pipeline onComplete { result =>
        result match {
          case Success(status) =>
            changeState.invoke(TaskFinished(taskId, status))
          case Failure(err) =>
            changeState.invoke(TaskFailed(taskId, err.toString))
        }
        asyncClose.invoke(())
      }
    }

    override def postStop() = {
      instance.foreach{ eid => executor.stop(eid)}
    }

    setHandler(states, GraphStageLogic.IgnoreTerminateOutput)

  }
}

object Builder {

  sealed trait BuildState {
    def taskId: TaskId
  }
  case class TaskStarting(taskId: TaskId, taskDef: TaskDef[Environment]) extends BuildState
  case class TaskRunning(taskId: TaskId, execId: ExecutionId) extends BuildState

  case class TaskFinished(taskId: TaskId, status: Int) extends BuildState
  case class TaskFailed(taskId: TaskId, message: String) extends BuildState

}
