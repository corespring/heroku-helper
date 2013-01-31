package org.corespring.heroku.helper.shell

class MockShell(response: String = "") extends Shell {

  var cmds : List[String] = List()

  def run(cmd: String) = {
    cmds = cmds :+ cmd
    CmdResult(cmd,response,"",0)
  }

  def run(cmd: String, outHandler: (String => Unit), errHandler: (String => Unit)): CmdResult = {
    cmds = cmds :+ cmd
    CmdResult.empty
  }
}

