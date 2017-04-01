package io.crashbox.ci
package yaml

import org.scalatest._

class SimpleReadersSpec extends FlatSpec with Matchers with SimpleReaders {

  "SimpleReaders" should "convert yaml" in {
    assert(Yaml.parse("hello").convertTo[String] == "hello")
    assert(Yaml.parse("42").convertTo[Byte] == 42.toByte)
    assert(Yaml.parse("42").convertTo[Short] == 42.toShort)
    assert(Yaml.parse("42").convertTo[Int] == 42)
    assert(Yaml.parse("42").convertTo[Long] == 42l)
    assert(Yaml.parse("42.0").convertTo[Float] == 42f)
    assert(Yaml.parse("42.0").convertTo[Double] == 42.0)
    assert(Yaml.parse("true").convertTo[Boolean] == true)
    assert(Yaml.parse("false").convertTo[Boolean] == false)
  }

  "SimpleReaders" should "fail to convert invalid yaml" in {
    intercept[YamlFormatException](Yaml.parse("foo").convertTo[Boolean])
  }
}
