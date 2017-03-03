package io.crashbox.ci
package source

import java.io.File
import java.net.URL
import org.eclipse.jgit.api.Git
import scala.concurrent.Future

trait GitFetchers extends Fetchers {

  @deprecated("use git-specific execution context", "todo")
  implicit private val ec = scala.concurrent.ExecutionContext.global

  def fetch(from: URL, to: File): Future[File] = Future {
    Git.cloneRepository.setURI(from.toURI.toString).setDirectory(to).call()
    to
  }

}
