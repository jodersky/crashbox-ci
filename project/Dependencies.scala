package crashbox

import sbt._

object Dependencies {

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % "2.4.17"
  val akkaHttp = "com.typesafe.akka" %% "akka-http" % "10.0.4"
  val akkaHttpCore = "com.typesafe.akka" %% "akka-http-core" % "10.0.4"
  val akkaHttpSpray = "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.0"
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % "2.4.17"

  val jgitServer = "org.eclipse.jgit" % "org.eclipse.jgit.http.server" % "4.6.0.201612231935-r"
  val jgitArchive = "org.eclipse.jgit" % "org.eclipse.jgit.archive" % "4.6.0.201612231935-r"

  val dockerClient = "com.spotify" % "docker-client" % "8.1.1"

  val slick = "com.typesafe.slick" %% "slick" % "3.2.0"
  //"com.typesafe.slick" %% "slick-hikaricp" % "3.2.0"

  val scalatest = "org.scalatest" %% "scalatest" % "3.0.1"

  val yaml = "org.yaml" % "snakeyaml" % "1.18"

}
