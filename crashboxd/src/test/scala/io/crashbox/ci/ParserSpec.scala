package io.crashbox.ci

import org.scalatest._

class ParserSpec extends FlatSpec with Matchers {

  val build = """|tasks:
                 | main:
                 |  image: foo/bar
                 |  script: echo "hello world"
                 |""".stripMargin

  val parsed = BuildDef(
    Seq(TaskDef(DockerEnvironment("foo/bar"), "echo \"hello world\"")))

  "Parser" should "parse build definitions" in {
    assert(Parser.parse(build) == Parser.Success(parsed))
  }
}
