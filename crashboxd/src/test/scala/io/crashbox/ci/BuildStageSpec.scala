package io.crashbox.ci

import akka.actor.ActorSystem
import akka.stream.scaladsl.Keep
import akka.stream.{ ClosedShape, KillSwitch }
import akka.stream.scaladsl.{ GraphDSL, RunnableGraph, Sink, Source }
import akka.stream.{ ActorMaterializer, FanInShape2 }
import java.io.{ ByteArrayOutputStream, File }
import java.nio.file.Files
import org.scalatest._
import scala.concurrent.Await
import scala.concurrent.duration._

class BuildStageSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  implicit val system = ActorSystem("crashboxd-buildstage")
  implicit val materializer = ActorMaterializer()
  val executor = new DockerExecutor


  override def beforeAll(): Unit = {
    DockerUtil.ensureImage(executor.dockerClient)
  }

  override def afterAll(): Unit = {
    assert(executor.clean(), "Spawned containers were not removed")
    system.terminate()
  }


  "BuildStage" should "have a test!" in {
    IOUtil.withTemp{ (dir, out) =>

      val taskDef = TaskDef(DockerEnvironment("crashbox"), "sleep 10; exit 0")
      val resultSink = Sink.foreach[Builder.BuildState](x => println(x))

      val stage = new BuildSource(
        TaskId("build", 0),
        taskDef,
        executor,
        dir,
        out
      )
      val src = Source.fromGraph(stage)

      //val done = src.toMat(resultSink)(Keep.right).run()

      //executor.start("crashbox", "sleep 10000", dir, out)
      Thread.sleep(1000)
      assert(executor.clean())
      //Await.ready(done, 30.seconds)
      println("eot")
    }
  }
}
