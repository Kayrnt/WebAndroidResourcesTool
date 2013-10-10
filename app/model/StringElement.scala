package model

case class StringElement (name : String, text : String) {
  override def toString: String = {
    return "StringElement [name=" + name + ", text=" + text + "]"
  }
}