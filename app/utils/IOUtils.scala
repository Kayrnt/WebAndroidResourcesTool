package utils

import java.io.{FilenameFilter, File}
import java.lang.String

/**
 * User: Kayrnt
 * Date: 17/08/13
 * Time: 12:13
 */
object IOUtils {

  def subdirectories(path: String): Array[String] = {
    val root = new File(path)
    val directories = root.list(new FilenameFilter() {
      def accept(dir: File, name: String): Boolean = {
        new File(dir, name).isDirectory;
      }
    })
    directories
  }
}
