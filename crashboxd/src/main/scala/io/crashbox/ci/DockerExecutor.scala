package io.crashbox.ci

import java.io.{File, OutputStream}
import java.util.UUID

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor.ActorSystem
import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient.{
  AttachParameter,
  ListContainersParam
}
import scala.util.Random
import com.spotify.docker.client.LogStream
import com.spotify.docker.client.exceptions.ContainerNotFoundException
import com.spotify.docker.client.messages.{ContainerConfig, HostConfig}
import com.spotify.docker.client.messages.HostConfig.Bind

object DockerExecutor {

  case class ExecutionId(id: String) extends AnyVal

  def containerUser = "crashbox"
  def containerWorkDirectory = "/home/crashbox"
  def containerKillTimeout = 5.seconds

}

class DockerExecutor(uri: String = "unix:///run/docker.sock")(
    implicit system: ActorSystem) {
  import DockerExecutor._
  import system.log

  val dockerClient = {
    val c = DefaultDockerClient.builder().uri(uri).build()
    system.registerOnTermination {
      c.close()
    }
    c
  }

  private val label = "crashbox-executor" -> UUID.randomUUID().toString

  def start(
      image: String,
      script: String,
      buildDirectory: File,
      out: OutputStream
  ): Future[ExecutionId] =
    Future {
      val volume = Bind
        .builder()
        .from(buildDirectory.getAbsolutePath)
        .to(containerWorkDirectory)
        .build()
      val hostConfig = HostConfig.builder().binds(volume).build()
      val containerConfig = ContainerConfig
        .builder()
        .labels(Map(label).asJava)
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
      system.dispatcher execute new Runnable {
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
    }(system.dispatcher)

  def result(id: ExecutionId): Future[Int] =
    Future {
      log.debug(s"Waiting for container $id to exit")
      val res: Int = dockerClient.waitContainer(id.id).statusCode()
      stop(id)
      res
    }(system.dispatcher)

  def stop(id: ExecutionId): Unit = {
    try {
      log.debug(s"Stopping container $id")
      dockerClient.stopContainer(id.id,
                                 containerKillTimeout.toUnit(SECONDS).toInt)
      log.debug(s"Removing container $id")
      dockerClient.removeContainer(id.id)
    } catch {
      case _: ContainerNotFoundException => // build already cancelled
    }
  }

  def clean(): Boolean = {
    val stale = dockerClient
      .listContainers(
        ListContainersParam.withLabel(label._1, label._2)
      )
      .asScala
    stale.isEmpty || {
      stale.foreach { container =>
        log.warning(s"Removing stale container ${container.id}")
        dockerClient.killContainer(container.id)
        dockerClient.removeContainer(container.id)
      }
      false
    }
  }

}
