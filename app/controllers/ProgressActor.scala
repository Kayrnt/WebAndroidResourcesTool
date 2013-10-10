package controllers

import utils.{ProcessUtils}
import java.io.File
import play.api._
import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._
import java.util.concurrent.TimeoutException

/**
 * User: Kayrnt
 * Date: 22/09/13
 * Time: 18:55
 */


case class Enum(enumerator: Enumerator[JsValue])

case class Percent(percent: Int)

case class Message(message: String, percent: Int)

case class Init(channel: Concurrent.Channel[JsValue], directory: String, uuid : String)

case class FileNumber(number: Int)

/**
 * Actor dedicated to monitor the progression and push it the Websocket
 */

object ProgressActor {
  val ITEM_TREATED = 0
}

class ProgressActor extends Actor {
  var progressChannel: Concurrent.Channel[JsValue] = null
  var directoryPath: String = null
  var uuid : String = null
  var itemTreatedCounter = 0
  var totalItemToTreatCounter: Double = 0

  /**
   * helper method to create messages
   */
  def sendProgress() = {
//    Logger.debug("file treated : " + fileTreatedCounter)
    val percent = (itemTreatedCounter / totalItemToTreatCounter * 100).asInstanceOf[Int]
    percent compare  100 match {
      case -1 => self ! Message("In progress", percent)
      case 0 => self ! {
        Logger.debug("Done !")
        self ! Message("Done", percent)
        progressChannel.push(createNewArchive())
      }
      case 1 => Logger.debug("Something is wrong !")
    }
  }

  def receive = {
    case number: Int => {
      if (number == ProgressActor.ITEM_TREATED) {
        itemTreatedCounter += 1
        sendProgress()
      }
    }

    case FileNumber(number) => {
      totalItemToTreatCounter = number
      Logger.debug("total file to treat : " + totalItemToTreatCounter)
    }

    case Message(text, percent) => {
      val json: JsValue = Json.toJson(
        Map(
          "value" -> JsNumber(percent),
          "text" -> JsString(text)
        )
      )
      progressChannel.push(json)
    }

    case Init(channel, directoryPath, uuid) => {
      this.progressChannel = channel
      this.directoryPath = directoryPath
      this.uuid = uuid
      val started = itemTreatedCounter != 0
      if (!started) {
        StringToolProcess.start(directoryPath, this)
      }
      else sendProgress()
    }
  }

  def createNewArchive(): JsValue = {
    //unzip
    Logger.debug("zipping...")
    val file : File = new File(directoryPath);
    val command = "zip code.zip -r ."
    Logger.debug(command)
    Logger.debug("in "+file.getAbsolutePath)
    try {
      ProcessUtils.executeCommandLine(command, 5000, file)
    }
    catch {
      case e: TimeoutException => {
        //failed unzip -> success false
        return Json.toJson(
          Map(
            "done" -> JsBoolean(false)
          )
        )
      }
    }

    Json.toJson(
      Map(
        "done" -> JsBoolean(true),
        "uuid" -> JsString(uuid)
      )
    )
  }

}

