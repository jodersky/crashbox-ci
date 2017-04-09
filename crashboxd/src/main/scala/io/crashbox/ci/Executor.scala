package io.crashbox.ci

import java.io.{ File, OutputStream }
import scala.concurrent.Future


trait Executor[E <: Environment] {

  def start(
    environment: E,
    script: String,
    buildDirectory: File,
    out: OutputStream
  ): Future[ExecutionId]

  def result(id: ExecutionId): Future[Int]

  def stop(id: ExecutionId): Unit

}
