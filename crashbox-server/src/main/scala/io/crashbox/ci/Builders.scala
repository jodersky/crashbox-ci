package io.crashbox.ci

import java.io.{File, OutputStream}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._

import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient.{
  AttachParameter,
  ListContainersParam
}
import com.spotify.docker.client.LogStream
import com.spotify.docker.client.exceptions.ContainerNotFoundException
import com.spotify.docker.client.messages.{ContainerConfig, HostConfig}
import com.spotify.docker.client.messages.HostConfig.Bind

trait Builders { core: Core =>

  val dockerClient =
    DefaultDockerClient.builder().uri("unix:///run/docker.sock").build()

  core.system.registerOnTermination {
    dockerClient.close()
  }

  def containerUser = "crashbox"
  def containerWorkDirectory = "/home/crashbox"
  def containerKillTimeout = 10.seconds

  case class ContainerId(id: String) {
    override def toString = id
  }

  def startBuild(image: String,
                 script: String,
                 dir: File,
                 out: OutputStream): Future[ContainerId] =
    Future {
      val volume = Bind
        .builder()
        .from(dir.getAbsolutePath)
        .to(containerWorkDirectory)
        .build()
      val hostConfig = HostConfig.builder().binds(volume).build()
      val containerConfig = ContainerConfig
        .builder()
        .labels(Map("crashbox" -> "build").asJava)
        .hostConfig(hostConfig)
        .tty(true) // combine stdout and stderr into stdout
        .image(image)
        .user(containerUser)
        .workingDir(containerWorkDirectory)
        .entrypoint("/bin/sh", "-c")
        .cmd(script)
        .build()
      val container = dockerClient.createContainer(containerConfig).id

      log.debug(s"Starting container $container")
      dockerClient.startContainer(container)

      log.debug(s"Attaching log stream of container $container")
      blockingDispatcher execute new Runnable {
        override def run() = {
          var stream: LogStream = null
          try {
            stream = dockerClient.attachContainer(
              container,
              AttachParameter.LOGS,
              AttachParameter.STDOUT,
              AttachParameter.STREAM
            )
            stream.attach(out, null, true)
          } finally {
            if (stream != null) stream.close()
          }
        }
      }
      ContainerId(container)
    }(blockingDispatcher)

  def waitBuild(id: ContainerId): Future[Int] =
    Future {
      log.debug(s"Waiting for container $id to exit")
      val res: Int = dockerClient.waitContainer(id.id).statusCode()
      cancelBuild(id)
      res
    }(blockingDispatcher)

  def cancelBuild(id: ContainerId): Unit = {
    log.debug(s"Stopping container $id")
    try {
      dockerClient.stopContainer(id.id,
                                 containerKillTimeout.toUnit(SECONDS).toInt)
      dockerClient.removeContainer(id.id)
    } catch {
      case _: ContainerNotFoundException => // build already cancelled
    }
  }

  def reapDeadBuilds(): Unit = {
    val stale = dockerClient
      .listContainers(
        ListContainersParam.withLabel("crashbox"),
        ListContainersParam.withStatusExited()
      )
      .asScala
    stale.foreach { container =>
      dockerClient.removeContainer(container.id())
    }
  }

}
