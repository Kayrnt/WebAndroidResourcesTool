package controllers

import utils.{IOUtils}
import java.io._
import java.lang.{StringBuilder, Boolean, String}
import model.{StringElement, AndroidStringRessource, ResourcesElement}
import scala.collection.mutable.ArrayBuffer
import scala.concurrent._
import ExecutionContext.Implicits.global
import play.api.Logger

/**
 * User: Kayrnt
 * Date: 08/10/13
 * Time: 16:45
 */
object StringToolProcess {

  def start(directoryPath: String, progressActor: ProgressActor) = {
    new Thread(new Runnable {
      def run() {
        Logger.debug("launching the process !")
        //check if correctly extracted
        val directories = IOUtils.subdirectories(directoryPath)
        //case extracted at the root
        if (directories.length > 1) {
          if (directories.contains("res")) {
            new StringToolProcess(directoryPath + File.separator + "res", progressActor)
          }
        }
        //case extracted
        else if (directories.length == 1) {
          //lets try to apply the lib
          new StringToolProcess(directoryPath + File.separator + directories(0), progressActor)
        }
      }
    }).start()
  }
}

class StringToolProcess(path: String, actorC: ProgressActor) {

  var stringsFile: ArrayBuffer[AndroidStringRessource] = new ArrayBuffer[AndroidStringRessource](10)
  var start: Long = 0L
  var actor: ProgressActor = actorC

  var resPath: String = (if (path == null) "" else path + "/") + "res"
  resPath = resPath.replaceAll("//", "/")
  Logger.debug("searching at path : " + resPath)
  start = System.currentTimeMillis
  val file: File = new File(resPath)
  if (!file.exists) {
    Logger.debug("Resources of project directory not found...")
  }
  else {
    try {
      visitAllFiles(file)
      syncStrings
    }
    catch {
      case e: Exception => {
        print("exception : " + e.getCause.getMessage)
        e.printStackTrace()
      }
    }
  }

  //Parse file and init our structures
  private def readFile(file: File) {
    val stringElem = scala.xml.XML.loadFile(file)
    stringsFile += new AndroidStringRessource(file, new ResourcesElement((stringElem \ "string").map {
      stringNode =>
        new StringElement((stringNode \ "@name").text, stringNode.text)
    }))
    Logger.debug("reading : " + file.getParent + "/" + file.getName)
  }

  //no args version of files process because we are at the root
  private def visitAllFiles(dir: File) {
    visitAllFiles(dir, null)
  }

  // Process only files under dir
  private def visitAllFiles(dir: File, parent: String) {
    if (dir.isDirectory) {
      val children: Array[String] = dir.list
      children.foreach({
        child =>
          visitAllFiles(new File(dir, child), dir.getName)
      })
    }
    else {
      readStrings(dir, parent)
    }
  }

  private def readStrings(file: File, parent: String) {
    //we read the file only if it's a strings.xml
    if (file.getName == "strings.xml") {
      readFile(file)
    }
  }

  private def syncStrings {
    Logger.debug("number of standard XML : " + stringsFile.size)
    var standardXML: AndroidStringRessource = null
    stringsFile.foreach(
      current =>
        if (current.file.getAbsolutePath.endsWith("res/values/strings.xml")) {
          standardXML = current
        })

    if (standardXML == null) {
      Logger.debug("No standard strings.xml found in res/values/ of this project")
      Logger.debug("The program found : " + stringsFile.size + " values files.")
      stringsFile.foreach(
        element =>
          Logger.debug("path for element is " + element.file.getAbsolutePath)
      )
    }
    else {
      Logger.debug("default xml strings found...")
    }

    stringsFile -= standardXML
    val standardStrings: Seq[StringElement] = standardXML.resources.strings
    val standardStringsSize: Int = standardStrings.size
    Logger.debug("strings : " + standardStrings.size)
    stringsFile.foreach(
      current =>
        current.parts = new Array[String](standardStringsSize))

    actor.self.tell(new FileNumber(standardStrings.size), actor.self)

    var futures: Seq[Future[Any]] = Seq()
    for (i <- 0 until standardStrings.length) {
      val current = standardStrings(i)
      val f = future {
        checkElementIsInOtherStringsXML(current, i)
      }
      futures = futures :+ f
    }

    Future.sequence(futures).onComplete {
      case _ =>
        mergeStrings
        closeAndroidXMLStrings
        Logger.debug("time consumed :" + (System.currentTimeMillis - start) + " ms")
    }
  }

  def checkElementIsInOtherStringsXML(stringElement: StringElement, position: Int) {
    Logger.debug("checkElementIsInOtherStringsXML (" + position + ")")

    stringsFile.foreach(
      resource => {
        val currentList: Seq[StringElement] = resource.resources.strings
        val parts: Array[String] = resource.parts
        //It's a comment so we just rename it and print it properly
        if (stringElement.name.startsWith("__comment_")) {
          parts(position) = "<!-- " + stringElement.text + " -->\n"
        }
        else {
          currentList.find(item => item.name == stringElement.name).fold[Any]({
            parts(position) = "<string name=\"" + stringElement.name + "\"></string>\n"
          }) {
            case item: StringElement =>
              parts(position) = "<string name=\"" + item.name + "\">" + item.text + "</string>\n";
          }
        }
      })

//    Logger.debug("telling actor... (" + position + ")")
    actor.self.tell(ProgressActor.ITEM_TREATED, actor.self)
    Logger.debug("actor -> file treated")


  }

  def mergeStrings {
    Logger.debug("merged strings")
    stringsFile.foreach(
      resource => {
        val builder: StringBuilder = new StringBuilder
        val parts: Array[String] = resource.parts
//        Logger.debug("merged strings : "+parts.size)
        parts.foreach(
          part => {
//            Logger.debug("part : " + part)
            builder.append(part)
          }
        )
        resource.transformed = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n" + builder.toString + "</resources>"
      })
  }

  def closeAndroidXMLStrings {
    Logger.debug("close Android XML Strings")
    stringsFile.foreach(
      resource =>
        try {
          val fstream: FileWriter = new FileWriter(resource.file)
          val out: BufferedWriter = new BufferedWriter(fstream)
          out.write(resource.transformed)
          out.close
        }
        catch {
          case e: Exception => {
            Logger.error("Error: " + e.getMessage)
          }
        })
  }

  //  def reset {
  //    backup = true
  //    revert = false
  //  }


}
