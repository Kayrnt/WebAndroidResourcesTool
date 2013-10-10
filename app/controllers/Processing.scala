package controllers

import utils.{ProcessUtils, IOUtils}
import java.io.File
import play.api.mvc._
import akka.actor._
import scala.concurrent.duration._

import play.api.libs.iteratee._

import akka.util.Timeout

import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import java.util.concurrent.TimeoutException
import play.api.libs.iteratee.Concurrent.Channel

object Processing extends Controller {

  implicit val timeout = Timeout(1 second)
  implicit val system = ActorSystem()

  def transform = WebSocket.using[JsValue] {
    request => Processing.start(request)
  }

  def startAkka(actor: ActorRef, channel: Channel[JsValue], directoryPath: String, uuid : String) = {
    actor ! Init(channel, directoryPath, uuid)
  }

  def start(request: RequestHeader): (Iteratee[JsValue, _], Enumerator[JsValue]) = {
    val (progressEnumerator, progressChannel) = Concurrent.broadcast[JsValue]

    val session = request.session;
    val directoryPath = session.get("directory").getOrElse(null)
    val uuid = session.get("uuid").getOrElse(null)
    if (directoryPath == null || uuid == null) return (null, null)

    val iteratee = Iteratee.skipToEof[JsValue].map {
      _ => println("Disconnected")
    }

    val actorPath: ActorPath = system / uuid;
    val actorSelection = system.actorSelection(actorPath);
    //3 seconds might be a bit long for local resolution but
    val future = actorSelection.resolveOne(3 seconds);
    //in case we don't find it, we probably didn't create it (in fact, it's the most common case because we might not have already started the job)
    future.onFailure {
      case t => println(t.getMessage)
        startAkka(system.actorOf(Props[ProgressActor], uuid), progressChannel, directoryPath, uuid)
    }
    //in case we found it
    future.map {
      actor => startAkka(actor, progressChannel, directoryPath, uuid)
    }

    (iteratee, progressEnumerator)
  }

}