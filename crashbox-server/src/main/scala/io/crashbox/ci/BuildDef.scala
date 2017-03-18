package io.crashbox.ci

case class BuildDef(
  image: String,
  script: String
)

case class ParseError(message: String)
