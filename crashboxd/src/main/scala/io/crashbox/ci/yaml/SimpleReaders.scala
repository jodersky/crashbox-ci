package io.crashbox.ci
package yaml

import scala.util.Try

trait SimpleReaders {

  class YamlStringReader[A](expected: String)(extract: String => Option[A])
      extends YamlReader[A] {
    def read(yml: YamlValue) = yml match {
      case YamlString(value) =>
        extract(value) match {
          case Some(a) => a
          case None =>
            throw new YamlFormatException(
              s"""expected $expected, but found string type "$value"""")
        }
      case _ => formatError(yml, expected)
    }
  }

  implicit object valueReader extends YamlReader[YamlValue] {
    def read(yaml: YamlValue) = yaml
  }

  implicit object stringReader
      extends YamlStringReader[String]("string")(s => Some(s))

  implicit object byteReader
      extends YamlStringReader[Byte]("byte")(s => Try { s.toByte }.toOption)
  implicit object shortReader
      extends YamlStringReader[Short]("short")(s => Try { s.toShort }.toOption)
  implicit object intReader
      extends YamlStringReader[Int]("integer")(s => Try { s.toInt }.toOption)
  implicit object longReader
      extends YamlStringReader[Long]("long")(s => Try { s.toLong }.toOption)
  implicit object floatReader
      extends YamlStringReader[Float]("float")(s => Try { s.toFloat }.toOption)
  implicit object doubleReader
      extends YamlStringReader[Double]("double")(s =>
        Try { s.toDouble }.toOption)
  implicit object booleanReader
      extends YamlStringReader[Boolean]("boolean")(s =>
        Try { s.toBoolean }.toOption)

}
