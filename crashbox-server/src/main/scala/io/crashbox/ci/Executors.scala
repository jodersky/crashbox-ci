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

trait Executors { core: Core =>

  val dockerClient =
    DefaultDockerClient.builder().uri("unix:///run/docker.sock").build()

  core.system.registerOnTermination {
    dockerClient.close()
  }

  def containerUser = "crashbox"
  def containerWorkDirectory = "/home/crashbox"
  def containerKillTimeout = 10.seconds

  case class ExecutionId(containerId: String) {
    override def toString = containerId
  }

  def startExecution(
      image: String,
      script: String,
      dir: File,
      out: OutputStream
  ): Future[ExecutionId] =
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
      ExecutionId(container)
    }(blockingDispatcher)

  def waitExecution(id: ExecutionId): Future[Int] =
    Future {
      log.debug(s"Waiting for container $id to exit")
      val res: Int = dockerClient.waitContainer(id.containerId).statusCode()
      cancelExecution(id)
      res
    }(blockingDispatcher)

  def cancelExecution(id: ExecutionId): Unit = {
    try {
      log.debug(s"Stopping container $id")
      dockerClient.stopContainer(id.containerId,
                                 containerKillTimeout.toUnit(SECONDS).toInt)
      log.debug(s"Removing container $id")
      dockerClient.removeContainer(id.containerId)
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
      log.warning(s"Removing stale container ${container.id}")
      dockerClient.removeContainer(container.id)
    }
  }

}
