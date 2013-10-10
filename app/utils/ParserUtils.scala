/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package utils

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.regex.Matcher
import java.util.regex.Pattern

object ParserUtils {
  /**
   * Build and return a {@link String} associed to the {@link java.io.InputStream}
   *
   * @throws java.io.IOException
   */

  var regex: Pattern = Pattern.compile("<!--(.*?)-->", Pattern.DOTALL)
  var commentId: Int = 0

  def getString(file: File): String = {
    val stream: InputStream = new FileInputStream(file)
    val reader: BufferedReader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))
    val json: StringBuilder = new StringBuilder
    var line: String = null
    while ((({
      line = reader.readLine; line
    })) != null) {
      json.append(transformIfNeeded(line))
    }
    reader.close
    return json.toString
  }

  def transformIfNeeded(xmlLine: String): String = {
    val matcher: Matcher = regex.matcher(xmlLine)
    if (matcher.find) {
      val comment: String = matcher.group(1)
      commentId += 1
      "<string name=\"" + "__comment_" + commentId + "\">" + comment + "</string>"
    }
    else xmlLine
  }


}