name := "crashbox"

//TODO: update scala version after
//https://github.com/scala-ide/scala-refactoring/issues/180 is fixed
crossScalaVersions in ThisBuild := List("2.12.0")
scalaVersion in ThisBuild := (crossScalaVersions in ThisBuild).value.head
scalacOptions in ThisBuild ++= Seq(
  "-deprecation",
  "-feature",
  //"-Xfatal-warnings",
  "-Xlint"
)
fork in ThisBuild := true
//cancelable in Global := true

lazy val root = (project in file(".")).aggregate(server)

lazy val server = (project in file("crashboxd"))
