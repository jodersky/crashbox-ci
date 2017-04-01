import crashbox._

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
  Dependencies.yaml,
  Dependencies.scalatest % Test
)
