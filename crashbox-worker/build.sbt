import crashbox.Dependencies

libraryDependencies ++= Seq(
  Dependencies.jgitArchive,
  Dependencies.jgitServer,
  Dependencies.scalatest % Test
)
