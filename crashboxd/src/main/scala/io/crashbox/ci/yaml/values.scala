package io.crashbox.ci
package yaml

sealed trait YamlValue {
  def convertTo[A: YamlReader]: A = implicitly[YamlReader[A]].read(this)
}
case class YamlString(value: String) extends YamlValue
object YamlString {
  val Empty = YamlString("")
}
case class YamlMap(fields: Map[String, YamlValue]) extends YamlValue
case class YamlSeq(elements: Seq[YamlValue]) extends YamlValue
