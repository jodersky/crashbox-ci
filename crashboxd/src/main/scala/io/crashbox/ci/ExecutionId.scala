package io.crashbox.ci

trait ExecutionId
case class DockerExecutionId(id: String) extends ExecutionId
