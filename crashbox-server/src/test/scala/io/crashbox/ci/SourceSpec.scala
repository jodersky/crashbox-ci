package io.crashbox.ci

import java.io.File
import java.nio.file.Files
import java.net.URL
import org.eclipse.jgit.api.Git
import scala.concurrent.duration._
import scala.concurrent.Await
import org.scalatest._

class SourceSpec extends FlatSpec with Matchers with Source with Core {

  val Timeout = 10.seconds

  def makeRepo(dir: File): Unit = {
    Git.init().setDirectory(dir).call()
    val file1 = new File(dir, "file1")
    file1.createNewFile()
    val file2 = new File(dir, "file2")
    file2.createNewFile()
    Git.open(dir).add().addFilepattern(".").call()
    Git.open(dir).commit().setMessage("initial commit").call()
  }

  "GitFetchers" should "be able to clone a local repository" in {
    TestUtil.withTempDir{ remote =>
      makeRepo(remote)
      TestUtil.withTempDir { local =>
        val cloned = Await.result(fetchSource(remote.toURI().toURL(), local), Timeout)
        assert(cloned.listFiles().length == 3)
      }
    }
  }

}
