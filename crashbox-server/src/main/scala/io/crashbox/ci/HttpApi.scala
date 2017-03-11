package io.crashbox.ci

import java.net.URL
import java.util.UUID

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.model.HttpEntity.ChunkStreamPart
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Source => Src}
import akka.stream.scaladsl.StreamConverters
import spray.json._

trait HttpApi { self: Core with Schedulers with Storage =>

  val endpoint = "api"

  case class Request(url: URL) {}

  object Protocol extends DefaultJsonProtocol {
    val urlReader = new JsonReader[URL] {
      override def read(js: JsValue) = js match {
        case JsString(str) => new URL(str)
        case _ => deserializationError("Expected valid url string")
      }
    }
    val urlWriter = new JsonWriter[URL] {
      override def write(url: URL) = JsString(url.toString())
    }
    implicit val urlFormat: JsonFormat[URL] = jsonFormat(urlReader, urlWriter)
    implicit val request = jsonFormat1(Request)
  }
  import Protocol._

  implicit val toResponseMarshaller: ToResponseMarshaller[Src[String, Any]] =
    Marshaller.opaque { items =>
      val data = items.map(item => ChunkStreamPart(item.toString + "\n"))
      HttpResponse(
        entity = HttpEntity.Chunked(ContentTypes.`text/plain(UTF-8)`, data))
    }

  def httpApi: Route = pathPrefix(endpoint) {
    path("submit") {
      post {
        entity(as[Request]) { req =>
          val scheduled = scheduleBuild(req.url).map(_.toString())
          complete(scheduled)
        }
      }
    } ~
      path(Segment / "cancel") { buildId =>
        post {
          cancelBuild(UUID.fromString(buildId))
          complete(204 -> None)
        }
      } ~
      path(Segment / "logs") { buildId =>
        get {
          val src = StreamConverters
            .fromInputStream(() => readLog(UUID.fromString(buildId), 0))
            .map { bs =>
              bs.utf8String
            }
          complete(src)
        }
      }
  }

}
