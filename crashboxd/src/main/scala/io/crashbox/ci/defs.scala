package io.crashbox.ci

sealed trait Environment
case class DockerEnvironment(image: String) extends Environment

case class TaskDef[+E <: Environment](environment: E, script: String)
case class BuildDef(tasks: Seq[TaskDef[_]])
