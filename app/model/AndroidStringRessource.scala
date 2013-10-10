package model

import java.io.File

class AndroidStringRessource(var file: File, var resources: ResourcesElement) {
  var parts: Array[String] = null
  var transformed: String = null
}