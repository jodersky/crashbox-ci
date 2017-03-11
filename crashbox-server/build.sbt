import crashbox.Dependencies

libraryDependencies ++= Seq(
  Dependencies.akkaActor,
  Dependencies.akkaHttp,
  Dependencies.akkaHttpCore,
  Dependencies.akkaHttpSpray,
  Dependencies.akkaStream,
  Dependencies.jgitArchive,
  Dependencies.jgitServer,
  Dependencies.dockerClient,
  Dependencies.slick,
  "com.h2database" % "h2" % "1.4.193",
  Dependencies.scalatest % Test
)
