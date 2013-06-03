package org.corespring.heroku.helper.shell

import scala.sys.process._

/** Shell behaviour */
trait Shell {
  /** Execute a command and return the response */
  def run(cmd: String): CmdResult

  def run(cmd: String, outHandler: (String => Unit), errHandler: (String => Unit)): CmdResult
}


case class CmdResult(name: String, out: String, err: String, exitCode: Int)

object CmdResult{
  def empty : CmdResult = CmdResult("","","",0)
}

/** Wraps system.Process calls
  *
  */
object Shell extends Shell {

  class SimpleLogger extends ProcessLogger {

    var outLog: String = ""
    var errLog: String = ""

    def buffer[T](f: => T): T = f

    def out(s: => String) {
      outLog += (s + "\n")
    }

    def err(s: => String) {
      errLog += (s + "\n")
    }
  }

  class WithHandlerLogger(outHandler:(String=>Unit), errHandler:(String=>Unit)) extends SimpleLogger{

    override def out (s : => String){
      super.out(s)
      outHandler(s)
    }
    override def err(s : => String){
      super.err(s)
      errHandler(s)
    }
  }

  /** Runs shell command
   * @param cmd - the command to run
   * @param outHandler - a callback for handling the process output
   * @param errHandler - a callback for handling the process errors
   * @return a CmdResult
   */
  def run(cmd: String, outHandler: (String => Unit), errHandler: (String => Unit)): CmdResult = {
    val logger: WithHandlerLogger = new WithHandlerLogger(outHandler, errHandler)
    val exitCode = cmd ! logger
    CmdResult(cmd, logger.outLog, logger.errLog, exitCode)
  }

  /** Run a shell command and return the result of the command
    */
  def run(cmd: String): CmdResult = {
    val logger: SimpleLogger = new SimpleLogger
    val exitCode = cmd ! logger
    CmdResult(cmd, logger.outLog, logger.errLog, exitCode)
  }
}
