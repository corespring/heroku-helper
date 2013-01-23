package org.corespring.herokuHelper

import grizzled.cmd._
import handlers._
import log.{logger, Debug}
import models.GlobalConfig
import java.io.File
import models.netrc


object CLI extends App {

  class Cmd(apiKey: String) extends CommandInterpreter("heroku-helper") {


    override def StartCommandIdentifier = "abcdefghijklmnopqrstuvwxyz" +
      "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
      "0123456789.-"

    val handlers: List[CommandHandler] = List(
      new AboutHandler,
      new ExitHandler,
      new ViewReposHandler,
      new PushHandler,
      new RollbackHandler)
  }

  import grizzled.file.util.joinPath
  import grizzled.string.util.tokenizeWithQuotes

  val Header = {
    val raw = """
                |-------------------------------------------
                |Heroku Helper version: ${version}
                |-------------------------------------------
              """.stripMargin
    string.utils.interpolate(raw, {
      key => Version.version.toString
    })
  }

  val apiKey: Option[String] = netrc.apiKey(netrc.DefaultPath)

  def console(apiKey: String) = {
    val cmd = new Cmd(apiKey)
    logger.level = Debug
    logger.info(Header)
    logger.debug("using apiKey: " + apiKey + " from: " + netrc.DefaultPath)
    logger.info("available commands: " + cmd.handlers.map(_.CommandName).mkString(", "))
    logger.info("run `help command` for more info")
    cmd.mainLoop
  }

  def logError = {
    logger.info(Header)
    logger.error("No api key found - have you logged in with the heroku toolbelt yet?")
  }

  apiKey match {
    case Some(key) => console(key)
    case _ => logError
  }
}

