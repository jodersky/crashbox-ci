package io.crashbox.ci
package build

import java.io.{ File, OutputStream }
import scala.concurrent.Future

trait Builders {

  def build(
    workdir: File,
    stdout: OutputStream,
    stderr: OutputStream
  ): Future[Int]

}
