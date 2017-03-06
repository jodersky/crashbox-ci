import crashbox.Dependencies

libraryDependencies ++= Seq(
  Dependencies.akkaActor,
  Dependencies.akkaStream,
  Dependencies.jgitArchive,
  Dependencies.jgitServer,
  Dependencies.dockerClient,
  Dependencies.scalatest % Test
)
