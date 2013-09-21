package controllers

import play.api.mvc._

import java.io.{FileOutputStream, File}
import utils.{ProcessUtils, IOUtils}

import play.api.libs.iteratee._


import play.api.libs.json.{JsString, JsNumber, Json, JsValue}
import java.util.concurrent.TimeoutException

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
  //, channel : Concurrent.Channel[JsValue]
  def useArchive(data: (Session, File)) = {
    val session = data._1
    val archive = data._2
    val directoryPath = archive.getAbsoluteFile.getParentFile.getAbsolutePath
    //unzip
    val command = "unzip -n " + archive.getAbsolutePath + " -d " + directoryPath
    println(command)
    try {
      ProcessUtils.executeCommandLine(command, 5000)
    }
    catch {
      case e: TimeoutException => {

      }
    }
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


  def transform = WebSocket.using[JsValue] {
    request => StringTool.start

  }

  object StringTool {

    def start: (Iteratee[JsValue, _], Enumerator[JsValue]) = {
      val (progressEnumerator, progressChannel) = Concurrent.broadcast[JsValue]
      val iteratee = Iteratee.foreach[JsValue](

      {
        value => println("received : " + value)
          updateProgressTest(progressChannel)
      })
        .mapDone {
        _ => println("Disconnected")
      }

      (iteratee, progressEnumerator)
    }


    def updateProgressTest(progressChannel: Concurrent.Channel[JsValue]) = {
      new Thread(new Runnable {
        def run() {
          var progress = 0
          while (progress < 100) {
            notifyProgress(Message("in progress", progress), progressChannel);
            Thread.sleep(1000)
            progress += 5
          }
        }
      }).run
    }

    def notifyProgress(msg: Message, channel: Concurrent.Channel[JsValue]) {
      val json: JsValue = Json.toJson(
        Map(
          "value" -> JsNumber(msg.percent),
          "text" -> JsString(msg.message)
        )
      )
      channel.push(json)
    }

  }


}

case class Enum(enumerator: Enumerator[JsValue])

case class Percent(percent: Int)

case class Message(message: String, percent: Int)

case class Initialisation(state: Boolean)

