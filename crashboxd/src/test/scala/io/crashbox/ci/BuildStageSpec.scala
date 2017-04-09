package io.crashbox.ci

import akka.actor.ActorSystem
import akka.stream.{ ClosedShape, KillSwitch }
import akka.stream.scaladsl.{ GraphDSL, RunnableGraph, Sink, Source }
import akka.stream.{ ActorMaterializer, FanInShape2 }
import java.io.{ ByteArrayOutputStream, File }
import java.nio.file.Files
import org.scalatest._
import scala.concurrent.Await
import scala.concurrent.duration._

class BuildStageSpec extends FlatSpec with Matchers with DockerSuite{

  implicit val materializer = ActorMaterializer()

  val exec = new DockerExecutor

  def withTmp[A](action: (File, ByteArrayOutputStream) => A): A = {
    val dir = Files.createTempDirectory("crashbox-build-stage-test").toFile
    val out = new ByteArrayOutputStream(1024)
    try action(dir, out)
    finally dir.delete()
  }

  "BuildStage" should "have a test!" in {
    withTmp{ case (dir, out) =>
      val taskDef = TaskDef(DockerEnvironment("crashbox"), "sleep 100; exit 0")

      val resultSink = Sink.foreach[Builder.BuildState](x => println(x))

      val graph = RunnableGraph.fromGraph(GraphDSL.create(resultSink) {
        implicit b => sink =>
        import GraphDSL.Implicits._

        val builder = b.add(new BuildStage(exec, _ => dir, _ => out))

        val submissions = b.add(
          Source.repeat(TaskId("123", 2) -> taskDef))
        val cancellations = b.add(
          Source.tick(10.seconds, 10.seconds, TaskId("0", 0)))

        val ks = b.add(KillSwitch)

        submissions ~> builder.in0
        cancellations ~> builder.in1

        builder.out ~> sink

        ClosedShape
      })

      graph.run()
      Thread.sleep(30000)
      println("terminating")
      Await.result(system.terminate(), 60.seconds)
      println("teminated")
      Thread.sleep(5000)
      println("eot")
    }

  }
}
