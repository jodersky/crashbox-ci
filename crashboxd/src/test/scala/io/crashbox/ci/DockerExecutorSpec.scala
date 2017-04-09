package io.crashbox.ci

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerClient.ListContainersParam
import java.io.{ByteArrayOutputStream, File}
import java.nio.file.Files

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._

import akka.actor.ActorSystem
import org.scalatest._
import scala.util.Random


class DockerExecutorSpec
    extends FlatSpec
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  import IOUtil._

  val image = "crashbox"

  val timeout = 30.seconds

  implicit val system = ActorSystem("docker-test")
  import system.dispatcher
  val exec = new DockerExecutor

  override def beforeAll: Unit = {
    sys.addShutdownHook {
      println("------------------- fooooo")
      exec.clean()
    }
    DockerUtil.ensureImage(exec.dockerClient)
  }

  override def afterAll: Unit = {
    system.terminate()
  }

  override def afterEach: Unit = {
    assert(exec.clean(), "Spawned containers were not removed")
  }

  def run[A](script: String)(tests: (Int, File, String) => A): A = withTemp {
    case (dir, out) =>
    
    val awaitable = for (id <- exec.start(image, script, dir, out);
      status <- exec.result(id)) yield {
      status
    }
    val status = Await.result(awaitable, timeout)
    tests(status, dir, new String(out.toByteArray()).trim())
  }

  "DockerExecutor" should "return expected exit codes" in {
    run("true") {
      case (status, _, _) =>
        assert(status == 0)
    }
    run("false") {
      case (status, _, _) =>
        assert(status == 1)
    }
    run("nonexistant") {
      case (status, _, _) =>
        assert(status == 127)
    }
  }

  it should "print the expected output" in {
    run("echo hello world") {
      case (_, _, out) =>
        assert(out == "hello world")
    }
    run("echo hello world >&2") {
      case (_, _, out) =>
        assert(out == "hello world")
    }
    run("echo hello world > /dev/null") {
      case (_, _, out) =>
        assert(out == "")
    }
  }

  it should "create expected files" in {
    run("echo hello world > data") {
      case (_, dir, _) =>
        val data = Files
          .lines((new File(dir, "data")).toPath)
          .iterator()
          .asScala
          .mkString("\n")
        assert(data == "hello world")
    }
  }

  it should "allow cancellations" in {
    withTemp { case (dir, out) =>
      val script = "while true; do sleep 1; echo sleeping; done"

      val id = Await.result(exec.start(image, script, dir, out), timeout)
      val check = exec.result(id).map { res =>
        assert(res == 137)
      }
      exec.stop(id)
      Await.result(check, timeout)
    }
  }

}
