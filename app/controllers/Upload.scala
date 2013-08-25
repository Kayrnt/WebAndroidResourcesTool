package controllers

import play.api.mvc._

import java.io.{FileOutputStream, File}
import utils.{ProcessUtils, IOUtils}
import play.libs.Akka
import akka.actor._
import scala.concurrent.duration._

import play.api.libs.iteratee._

import akka.util.Timeout
import akka.pattern.ask

import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsString, JsNumber, Json, JsValue}

/**
 * User: Kayrnt
 * Date: 03/08/13
 * Time: 13:01
 */
object Upload extends Controller {

  /**
   * Store the body content into a file and session.
   *
   * @param to The file used to store the content.
   * @param session The updated session.
   */
  def sessionAndfile(to: File, session: Session): BodyParser[(Session, File)] =
    BodyParser("file and session, to=" + to + " and session = " + session) {
      request =>
        Iteratee.fold[Array[Byte], FileOutputStream](new FileOutputStream(to)) {
          (os, data) =>
            os.write(data)
            os
        }.map {
          os =>
            os.close()
            Right((session, to))
        }
    }

  //body parser to store file in "uuid"/"uuid".zip with a limit of 10 MB
  val storeInUserFile = {
    var finalUuid: (Session, String) = null;
    parse.maxLength(1024 * 10000,
      parse.using {
        request => {
          finalUuid = uuid(request)
          println("uuid : " + finalUuid)
          val file = new File("upload/" + finalUuid._2 + File.separator + finalUuid._2 + ".zip")
          val parent: File = file.getParentFile
          println("file : " + file.getAbsolutePath)
          if (!parent.exists && !parent.mkdirs) {
            throw new IllegalStateException("Couldn't create dir: " + parent)
          }
          println("to body parser");
          val fo: BodyParser[(Session, File)] = sessionAndfile(file, finalUuid._1);
          println("fo : " + fo);
          fo
        }
      })
  }


  //action handler receiving the the file uploaded and handling the response
  def fileUploader = Action(storeInUserFile) {
    request =>
      println("request : " + request)
      request.body.fold[Result](sizeExceeded, useArchive)
  }

  //return in case the size is too big
  def sizeExceeded(size: MaxSizeExceeded) = {
    println("File size exceeded " + size.length)
    BadRequest("File size exceeded")
  }

  //retrieve the uuid for the session and store a new one if required
  def uuid(request: RequestHeader) = {
    var session = request.session
    val uuid = session.get("uuid").getOrElse({
      println("uuid get or else -> else")
      val newUuid = java.util.UUID.randomUUID().toString()
      println("new uuid " + newUuid)
      session = session.+("uuid", newUuid)
      newUuid
    })
    (session, uuid)
  }

  //extract the archive and proceed
  def useArchive(data: (Session, File)) = {
    val session = data._1
    val archive = data._2
    val directoryPath = archive.getAbsoluteFile.getParentFile.getAbsolutePath
    //unzip
    val command = "unzip -n " + archive.getAbsolutePath + " -d " + directoryPath
    println(command)
    ProcessUtils.executeCommandLine(command, 5000)
    //check if correctly extracted
    val directories = IOUtils.subdirectories(directoryPath)
    //case extracted at the root
    if (directories.length > 1) {
      if (directories.contains("res")) {
        Main.main(directoryPath + File.separator + "res")
      }
    }
    //case extracted
    else if (directories.length == 1) {
      //lets try to apply the lib
      Main.main(directoryPath + File.separator + directories(0))
    }

    println("Ok !!!")

    Ok("File uploaded : " + archive.getName).withSession(session)
  }

  def fileUploaderGet = Action {
    Ok("upload servlet")
  }


  def transform = WebSocket.async[JsValue] {
    request => StringTool.start

  }

  object StringTool {

    implicit val timeout = Timeout(1 second)

    def start: scala.concurrent.Future[(Iteratee[JsValue, _], Enumerator[JsValue])] = {
      val stringTool = Akka.system.actorOf(Props[StringTool])

      (stringTool ? Initialisation(true)).map {

        case Enum(enumerator) => {
          println("initialisation of actor done... returning websocket elements")
          println("Enumerator : "+enumerator.map(
          value => println(value)
          ))

          val iteratee = Iteratee.foreach[JsValue](println).mapDone { _ =>
              println("Disconnected")
          }

          (iteratee, enumerator)
        }
      }
    }

  }

  class StringTool extends Actor {

    val (progressEnumerator, progressChannel) = Concurrent.broadcast[JsValue]

    def receive = {
      case Percent(percent) => {
        self ! Message(percent+"%", percent)
      }

      case Message(msg, percent) => {
        val json: JsValue = Json.toJson(
          Map(
            "value" -> JsNumber(percent),
            "text" -> JsString(msg)
          )
        )
        notifyProgress(json)
      }

      case Initialisation(state) => {
        sender ! Enum(progressEnumerator)
        new Thread(new Runnable {
          def run() {
            Thread.sleep(3000)
            self ! Message("starting...", 10)
          }
        }).run
    }

  }

  def notifyProgress(msg: JsValue) {
    println("notify progress")
    progressChannel.push(msg)
    println(" notify pushed")
  }

}

}

case class Enum(enumerator: Enumerator[JsValue])

case class Percent(percent: Int)

case class Message(message: String, percent: Int)

case class Initialisation(state: Boolean)

