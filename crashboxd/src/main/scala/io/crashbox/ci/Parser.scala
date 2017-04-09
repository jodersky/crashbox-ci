package io.crashbox.ci

import io.crashbox.ci.yaml._
import io.crashbox.ci.yaml.DefaultReaders._

object Parser {
  sealed trait Result
  case class Success(buildDef: BuildDef) extends Result
  case class Failure(error: String) extends Result

  implicit object TaskDefReader extends YamlReader[TaskDef[_]] {
    def read(value: YamlValue) = {
      val items = value.convertTo[Map[String, YamlValue]]
      val image = items
        .getOrElse("image",
                   throw new YamlFormatException("no image specified"))
        .convertTo[String]
      val script = items
        .getOrElse("script",
                   throw new YamlFormatException("no script specified"))
        .convertTo[String]
      TaskDef(DockerEnvironment(image), script)
    }
  }

  implicit object BuildDefReader extends YamlReader[BuildDef] {
    def read(value: YamlValue) = {
      val items = value.convertTo[Map[String, YamlValue]]
      val tasks = items
        .getOrElse("tasks",
                   throw new YamlFormatException("no tasks specified"))
        .convertTo[Map[String, TaskDef[_]]]
      BuildDef(tasks.values.toSeq)
    }
  }

  def parse(build: String): Result =
    try {
      Success(Yaml.parse(build).convertTo[BuildDef])
    } catch {
      case ex: YamlFormatException => Failure(ex.toString)
    }

}
