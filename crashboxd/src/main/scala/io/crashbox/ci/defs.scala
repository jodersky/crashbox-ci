package io.crashbox.ci

sealed trait Environment
case class DockerEnvironment(image: String) extends Environment

case class TaskDef(environment: Environment, script: String)
case class BuildDef(tasks: Seq[TaskDef])
