package io.crashbox.ci
package yaml

import org.scalatest._

class YamlSpec extends FlatSpec with Matchers {

  val yml = """|---
               |foo: bar
               |buz: qux
               |list:
               | - elem1
               | - elem2
               |map:
               | elem1: foo
               | elem2: bar
               | elem3:
               |""".stripMargin

  val tree = YamlMap(
    Map(
      "foo" -> YamlString("bar"),
      "buz" -> YamlString("qux"),
      "list" -> YamlSeq(
        Seq(
          YamlString("elem1"),
          YamlString("elem2")
        )),
      "map" -> YamlMap(
        Map(
          "elem1" -> YamlString("foo"),
          "elem2" -> YamlString("bar"),
          "elem3" -> YamlString("")
        ))
    ))

  "Yaml" should "parse valid yaml" in {
    assert(Yaml.parse(yml) == tree)
  }
}
