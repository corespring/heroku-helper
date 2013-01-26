package org.corespring.heroku.helper.shell

import sys.process.ProcessLogger

trait Shell {
  /** Execute a command and return the response */
  def run(cmd: String): CmdResult
}


case class CmdResult(name:String,out:String,err:String,exitCode:Int)


object Shell extends Shell {

  /** Run a shell command and return the output
    */
  def run(cmd: String): CmdResult = {

    var outLog: String = ""
    var errLog: String = ""

    val logger: ProcessLogger = new ProcessLogger {

      def buffer[T](f: => T): T = f

      def out(s: => String) {
        outLog += (s + "\n")
      }

      def err(s: => String) {
        errLog += (s + "\n")
      }
    }

    import scala.sys.process._

    val exitCode = cmd ! logger
    CmdResult(cmd,outLog,errLog,exitCode)
  }
}
