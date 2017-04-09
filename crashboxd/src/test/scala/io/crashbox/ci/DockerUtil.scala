package io.crashbox.ci

object DockerUtil {
  import IOUtil._
  import com.spotify.docker.client.DockerClient
  import java.io.File
  import java.nio.file.Files

  val defaultImage = "crashbox"

  def ensureImage(client: DockerClient): Unit = {
    println("Pulling base docker image for running docker tests")
    val baseImage = "debian:jessie-backports"
    client.pull(baseImage)

    withTempDir { dir =>
      println("Adapting base image for tests")
      val modifications = s"""|FROM $baseImage
                              |RUN adduser crashbox
                              |USER crashbox
                              |""".stripMargin
      Files.write((new File(dir, "Dockerfile")).toPath, modifications.getBytes)
      client.build(dir.toPath, defaultImage)
    }
  }

}
