package io.crashbox.ci
package build

import java.io.{ BufferedOutputStream, ByteArrayOutputStream, File }
import java.nio.file.Files
import java.net.URL
import org.eclipse.jgit.util.Paths
import scala.concurrent.duration._
import scala.concurrent.Await
import org.scalatest._

class ShellBuildersSpec extends FlatSpec with Matchers with ShellBuilders {

  val Timeout = 10.seconds

  def runScript(script: String): (Int, String, String) = {
    val stdout = new ByteArrayOutputStream(4096)
    val stderr = new ByteArrayOutputStream(4096)

    val result = TestUtil.withTempDir{ dir =>
      val exec = new File(dir, "crashbox")
      exec.createNewFile()
      Files.write(exec.toPath, script.getBytes)
      exec.setExecutable(true)

      Await.result(build(dir, stdout, stderr), Timeout)
    }
    stdout.close()
    stderr.close()

    (result, new String(stdout.toByteArray(), "utf-8"), new String(stderr.toByteArray(), "utf-8"))
  }

  "ShellBuilders" should "run a shell script" in {
    val script = """|#!/bin/sh
                    |echo "hello world"
                    |echo "foo" >&2
                    |""".stripMargin
    val (res, stdout, stderr) = runScript(script: String)

    assert(res == 0)
    assert(stdout == "hello world\n")
    assert(stderr == "foo\n")
  }

  it should "report a failed script" in {
    val script = """|#!/bin/sh
                    |exit 1
                    |""".stripMargin
    val (res, _, _) = runScript(script: String)

    assert(res == 1)
  }

}
