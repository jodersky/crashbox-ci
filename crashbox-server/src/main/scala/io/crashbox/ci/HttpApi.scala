package io.crashbox.ci

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.{
  ContentType,
  ContentTypes,
  HttpEntity,
  MediaTypes
}
import akka.http.scaladsl.model.HttpEntity.ChunkStreamPart
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.directives.Credentials
import akka.stream.scaladsl.{Flow, Keep, Sink}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Source => Src}
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import scala.util.{Failure, Success}
import akka.http.scaladsl.server._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import Directives._
import SprayJsonSupport._
import spray.json.{DefaultJsonProtocol, JsObject, JsString, JsValue}

trait HttpApi { self: Core with Schedulers with StreamStore =>

  val endpoint = "api"

  case class Request(url: String) {
    def buildId: String = {
      val bytes = MessageDigest.getInstance("SHA-256").digest(url.getBytes)
      bytes.map { byte =>
        Integer.toString((byte & 0xff) + 0x100, 16)
      }.mkString
    }
  }

  object Protocol extends DefaultJsonProtocol {
    implicit val request = jsonFormat1(Request)
  }
  import Protocol._

  implicit val toResponseMarshaller: ToResponseMarshaller[Src[BuildState, Any]] =
    Marshaller.opaque { items =>
      val data = items.map(item => ChunkStreamPart(item.toString + "\n"))
      HttpResponse(
        entity = HttpEntity.Chunked(ContentTypes.`text/plain(UTF-8)`, data))
    }

  def httpApi: Route = pathPrefix(endpoint) {
    path("submit") {
      entity(as[Request]) { req =>
        val source = Src
          .queue[BuildState](100, OverflowStrategy.fail)
          .mapMaterializedValue { q =>
            start(
              req.buildId,
              new URL(req.url),
              () => saveStream(req.buildId),
              state => q.offer(state)
            )
          }

        complete(source)
      }

    }
  }

}
