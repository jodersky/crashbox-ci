package crashbox

import sbt._

object Dependencies {

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % "2.4.17"
  val akkaHttpCore = "com.typesafe.akka" %% "akka-http-core" % "10.0.4"
  val akkaHttp = "com.typesafe.akka" %% "akka-http" % "10.0.4"

  val jgitServer = "org.eclipse.jgit" % "org.eclipse.jgit.http.server" % "4.6.0.201612231935-r"
  val jgitArchive = "org.eclipse.jgit" % "org.eclipse.jgit.archive" % "4.6.0.201612231935-r"

  val scalatest = "org.scalatest" %% "scalatest" % "3.0.1"

}
