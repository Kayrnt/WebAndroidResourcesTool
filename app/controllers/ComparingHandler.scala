package controllers

import model.StringElement
/**
 * User: Kayrnt
 * Date: 08/10/13
 * Time: 16:49
 */

class ComparingHandler(elementC : StringElement,
                       i : Int,
                       process : StringToolProcess)
  extends Runnable{

var element : StringElement = elementC
var position : Int = i
var stringProcess : StringToolProcess = process

  def run() {
    stringProcess.checkElementIsInOtherStringsXML(element, position);
  }

}
