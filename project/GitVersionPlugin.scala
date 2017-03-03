package crashbox

import scala.language.postfixOps
import sbt._
import sbt.Keys._

object GitVersionPlugin extends AutoPlugin {

  override def requires = plugins.CorePlugin
  override def trigger = allRequirements

  override def projectSettings = Seq(
    version := (
      "git describe --always --dirty=-SNAPSHOT --match v[0-9].*" #||
        "echo v0.0.0-SNAPSHOT" !!
    ).tail.trim
  )

}
