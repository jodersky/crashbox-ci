package io.crashbox.ci

import java.io.File
import java.net.URL

import scala.concurrent.Future

import org.eclipse.jgit.api.Git

trait Source { self: Core =>

  def fetchSource(from: URL, to: File): Future[File] =
    Future {
      log.debug(s"Cloning git repo from $from to $to")
      Git.cloneRepository.setURI(from.toURI.toString).setDirectory(to).call()
      to
    }(blockingDispatcher)

}
