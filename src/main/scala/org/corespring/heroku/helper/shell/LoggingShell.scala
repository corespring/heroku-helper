package org.corespring.heroku.helper.shell

class LoggingShell(response: String = "") extends Shell {

  var cmds : List[String] = List()


  def reset = cmds = List()

  def run(cmd: String) = {
    cmds = cmds :+ cmd
    println(cmd)
    CmdResult(cmd,response,"",0)
  }

  def run(cmd: String, outHandler: (String => Unit), errHandler: (String => Unit)): CmdResult = {
    cmds = cmds :+ cmd
    println(cmd)
    CmdResult.empty
  }
}

