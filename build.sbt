name := "crashbox"

crossScalaVersions in ThisBuild := List("2.12.1")
scalaVersion in ThisBuild := (crossScalaVersions in ThisBuild).value.head
scalacOptions in ThisBuild ++= Seq(
  "-deprecation",
  "-feature",
  "-Xfatal-warnings",
  "-Xlint"
)
fork in ThisBuild := true
cancelable in Global := true

lazy val root = (project in file(".")).aggregate(http)

lazy val http = (project in file("crashbox-http"))

lazy val worker = (project in file("crashbox-worker"))
