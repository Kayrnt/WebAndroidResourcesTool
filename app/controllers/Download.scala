package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import views._
import play.api.libs.json.JsValue
import java.io.File

/**
 * User: Kayrnt
 * Date: 09/10/13
 * Time: 14:08
 */
object Download extends Controller{

  def downloadProcessedFile (uuid : String) = Action {
    val file = new File("upload/"+uuid+"/code.zip")
    println("file : "+file.exists())
    Ok.sendFile(file)
  }

}
