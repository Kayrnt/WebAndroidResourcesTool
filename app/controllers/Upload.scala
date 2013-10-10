package controllers


import java.io.{FileOutputStream, File}
import utils.ProcessUtils

import play.api.mvc._

import play.api.libs.iteratee._


import play.api.mvc.MaxSizeExceeded
import play.api.libs.json._
import java.util.concurrent.TimeoutException
import org.apache.commons.io.{FileUtils}
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global


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

          else FileUtils.cleanDirectory(parent);

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
      session = session +("uuid", newUuid)
      newUuid
    })
    (session, uuid)
  }

  //extract the archive and proceed
  //, channel : Concurrent.Channel[JsValue]
  def useArchive(data: (Session, File)) = {
    var session = data._1
    val archive = data._2
    val directoryPath = archive.getAbsoluteFile.getParentFile.getAbsolutePath
    //unzip
    val command1 = "unzip -n " + archive.getAbsolutePath + " -d " + directoryPath
    println(command1)
    try {
      ProcessUtils.executeCommandLine(command1, 5000)
      archive.delete()
    }
    catch {
      case e: TimeoutException => {
        println("timeout")
        //failed unzip -> success false
        Ok(
          Json.toJson(
            Map(
              "success" -> JsBoolean(false),
              "uuid" -> JsString(session.get("uuid").getOrElse(""))
            )
          )
        ).as("text/html").withSession(session)
      }
    }

    session = session +("directory", directoryPath)

    println("Ok !!!")

    Ok(
      Json.toJson(
        Map(
          "success" -> JsBoolean(true),
          "uuid" -> JsString(session.get("uuid").getOrElse(""))
        )
      )
    ).as("text/html").withSession(session)
  }

  def fileUploaderGet = Action {
    Ok("upload servlet")
  }

}