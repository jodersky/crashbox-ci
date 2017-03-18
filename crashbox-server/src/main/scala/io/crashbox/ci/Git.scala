package io.crashbox.ci

import java.io.File
import java.net.URL

import scala.concurrent.Future

import org.eclipse.jgit.api.{Git => JGit}

object Git {

  def fetchSource(from: URL, to: File)(implicit core: Core): Future[File] = {
    import core._
    Future {
      log.debug(s"Cloning git repo from $from to $to")
      JGit.cloneRepository.setURI(from.toURI.toString).setDirectory(to).call()
      to
    }(blockingDispatcher)
  }

}
