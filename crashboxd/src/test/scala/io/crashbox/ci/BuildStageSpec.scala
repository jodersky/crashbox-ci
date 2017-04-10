package io.crashbox.ci

import java.io.{ByteArrayOutputStream, File, OutputStream}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

import Builder._
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, RunnableGraph, Sink, Source}
import org.scalatest._

class BuildStageSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  implicit val system = ActorSystem("crashboxd-buildstage")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  case class DummyEnv() extends Environment {
    val id = DummyId()
  }

  case class DummyId() extends ExecutionId {
    import DummyId._

    private var _state: State = Starting
    def state = _state.synchronized { _state }
    def state_=(value: State) = _state.synchronized { _state = value }

  }
  object DummyId {
    sealed trait State
    case object Starting extends State
    case object Running extends State
    case object Result extends State
    case object Stopped extends State
  }

  class DummyExecutor(
      startDelay: Duration = 0.seconds,
      resultDelay: Duration = 0.seconds,
      stopDelay: Duration = 0.seconds
  ) extends Executor[DummyEnv, DummyId] {

    override def start(env: DummyEnv,
                       script: String,
                       dir: File,
                       out: OutputStream) = Future {
      Thread.sleep(startDelay.toMillis)
      env.id.state = DummyId.Running
      env.id
    }

    override def result(id: DummyId) = Future {
      Thread.sleep(resultDelay.toMillis)
      id.state = DummyId.Result
      0
    }

    override def stop(id: DummyId) = {
      Thread.sleep(stopDelay.toMillis)
      id.state = DummyId.Stopped
    }

  }

  def dummySource(
      executor: DummyExecutor,
      env: DummyEnv
  ): Source[BuildState, NotUsed] = {
    val stage = new BuildSource(
      TaskId("dummy", 0),
      TaskDef(env, ""),
      executor,
      new File("nonexistant"),
      new ByteArrayOutputStream(0)
    )
    Source.fromGraph(stage)
  }

  "BuildStage" should "transition states and emit in the correct order" in {
    val delay = 0.5.seconds

    val executor = new DummyExecutor(delay, delay, delay)
    val env = new DummyEnv()

    val taskId = TaskId("dummy", 0)
    val taskDef = TaskDef(env, "dummy script")
    val stage = new BuildSource(
      taskId,
      taskDef,
      executor,
      new File("nonexistant"),
      new ByteArrayOutputStream(0)
    )
    val source = Source.fromGraph(stage)
    val eventFuture = source.toMat(Sink.seq)(Keep.right).run()

    Thread.sleep((delay / 2).toMillis)
    assert(env.id.state == DummyId.Starting)

    Thread.sleep(delay.toMillis)
    assert(env.id.state == DummyId.Running)

    Thread.sleep(delay.toMillis)
    assert(env.id.state == DummyId.Result)

    Thread.sleep(delay.toMillis)
    assert(env.id.state == DummyId.Stopped)

    val expectedEvents = Seq(
      TaskStarting(taskId, taskDef),
      TaskRunning(taskId, env.id),
      TaskFinished(taskId, 0)
    )
    val events = Await.result(eventFuture, 10.seconds)
    assert(events.toList === expectedEvents.toList)

  }

}
