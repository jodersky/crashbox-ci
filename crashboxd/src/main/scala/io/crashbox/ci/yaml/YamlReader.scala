package io.crashbox.ci
package yaml

trait YamlReader[A] {

  def read(yml: YamlValue): A

  protected def formatError(found: YamlValue, required: String) = {
    val foundType = found match {
      case _: YamlString => "string"
      case _: YamlSeq => "sequence"
      case _: YamlMap => "mapping"
    }

    throw new YamlFormatException(
      s"$found is of type $foundType, required: $required"
    )
  }

  protected def readError(node: YamlValue, msg: String) = {
    throw new YamlFormatException(node.toString + ": " + msg)
  }

}
