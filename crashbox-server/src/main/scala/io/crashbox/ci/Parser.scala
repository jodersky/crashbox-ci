package io.crashbox.ci

import java.io.File
import java.nio.file.Files

import scala.collection.JavaConverters._

object Parser {

  def defaultImage = "crashbox/default"

  def parseBuild(workdir: File): Either[BuildDef, ParseError] = {
    val file = new File(workdir, ".crashbox.txt")
    if (!file.exists()) {
      return Right(
        ParseError("No build configuration file .crashbox.txt found."))
    }

    val lines = Files.readAllLines(file.toPath).asScala.map(_.trim)

    val Pattern = """(\w+)\s*:\s*(.+)""".r

    val image = lines
      .collectFirst { case Pattern("image", s) => s }
      .getOrElse(defaultImage)
    val script = lines.collectFirst { case Pattern("script", s) => s }

    script match {
      case Some(s) => Left(BuildDef(image, s))
      case None =>
        Right(ParseError("No build script defined in configuration."))
    }
  }
}
