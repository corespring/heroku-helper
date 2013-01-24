package org.corespring.herokuHelper.shell

import sys.process.ProcessLogger

trait Shell {
  /** Execute a command and return the response */
  def run(cmd:String) : String
}

object Shell extends Shell {

  /** Run a shell command and return the output
    */
  def run(cmd: String): String = {

    var outLog: String = ""

    val logger: ProcessLogger = new ProcessLogger {

      def buffer[T](f: => T): T = f

      def out(s: => String) {
        outLog += (s + "\n")
      }

      def err(s: => String) {}
    }

    import scala.sys.process._

    cmd ! logger

    outLog

  }
}
