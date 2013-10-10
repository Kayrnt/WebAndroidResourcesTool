package utils

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeoutException

/**
 * User: Kayrnt
 * Date: 17/08/13
 * Time: 13:17
 */
object ProcessUtils {
  def executeCommandLine(commandLine: String, timeout: Long): Int = {
    return executeCommandLine(commandLine, timeout, null)
  }

  def executeCommandLine(commandLine: String, timeout: Long, dir: File): Int = {
    val runtime: Runtime = Runtime.getRuntime
    val process: Process = runtime.exec(commandLine, null, dir)
    val worker: ProcessUtils.Worker = new ProcessUtils.Worker(process)
    worker.start
    try {
      worker.join(timeout)
      if (worker.exit != -1) return worker.exit
      else throw new TimeoutException
    }
    catch {
      case ex: InterruptedException => {
        worker.interrupt
        Thread.currentThread.interrupt
        throw ex
      }
    }
    finally {
      process.destroy
    }
  }

  private class Worker(var process: Process) extends Thread {
    var exit: Int = -1

    override def run {
      try {
        exit = process.waitFor
      }
      catch {
        case ignore: InterruptedException => {
          return
        }
      }
    }
  }

}