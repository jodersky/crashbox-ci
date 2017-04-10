package io.crashbox.ci

import java.io.{File, OutputStream}

import scala.concurrent.Future

trait Executor[Env <: Environment, Id <: ExecutionId] {

  def start(
      environment: Env,
      script: String,
      buildDirectory: File,
      out: OutputStream
  ): Future[Id]

  def result(id: Id): Future[Int]

  def stop(id: Id): Unit

}
