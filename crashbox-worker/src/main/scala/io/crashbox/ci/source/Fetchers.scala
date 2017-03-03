package io.crashbox.ci
package source

import java.io.File
import java.net.URL
import scala.concurrent.Future

trait Fetchers {

  def fetch(from: URL, to: File): Future[File]

}
