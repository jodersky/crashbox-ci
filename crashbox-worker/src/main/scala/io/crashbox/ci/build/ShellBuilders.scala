package io.crashbox.ci
package build

import java.io.OutputStream
import scala.concurrent.Future
import scala.sys.process.{ Process, _ }
import java.io.{ File, InputStream }
import scala.concurrent.Future

trait ShellBuilders extends Builders {

  def pipe(is: InputStream, os: OutputStream) = {
    var n = 0
    val buffer = new Array[Byte](1024);
    while ({n = is.read(buffer); n > -1}) {
      os.write(buffer, 0, n);
    }
    os.close()
  }

  @deprecated("use git-specific execution context", "todo")
  implicit private val ec = scala.concurrent.ExecutionContext.global

  override def build(workdir: File, stdout: OutputStream, stderr: OutputStream): Future[Int] = {
    def ignore(in: OutputStream): Unit = ()
    val io = new ProcessIO(ignore, pipe(_, stdout), pipe(_, stderr))

    Future{
      Process("./crashbox", Some(workdir)).run(io).exitValue()
    }
  }

}
