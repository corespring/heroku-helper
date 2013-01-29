package org.corespring.heroku.helper.shell

class MockShell(response: String) extends Shell {
  def run(cmd: String) = CmdResult(cmd,response,"",0)
  def run(cmd: String, outHandler: (String => Unit), errHandler: (String => Unit)): CmdResult = CmdResult.empty
}
