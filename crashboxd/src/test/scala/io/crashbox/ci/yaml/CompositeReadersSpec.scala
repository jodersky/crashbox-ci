package io.crashbox.ci
package yaml

import org.scalatest._

class CompositeReadersSpec
    extends FlatSpec
    with Matchers
    with CompositeReaders
    with SimpleReaders {

  "CompositeReaders" should "convert yaml" in {
    assert(
      Yaml.parse("hello: world").convertTo[Map[String, String]] == Map(
        "hello" -> "world"))
    assert(
      Yaml.parse("hello: 42").convertTo[Map[String, Int]] == Map(
        "hello" -> 42))

    assert(Yaml.parse("- 42").convertTo[Seq[Int]] == Seq(42))
    assert(
      Yaml.parse("hello:\n - 42").convertTo[Map[String, Seq[Int]]] == Map(
        "hello" -> Seq(42)))
  }

}
