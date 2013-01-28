package org.corespring.heroku.helper

import grizzled.cmd._
import org.corespring.heroku.helper.handlers._
import log.{logger, Debug}
import models._
import java.io.File
import shell.{Shell, Git}
import scala.Some


object CLI extends App {

  class Console(apiKey: String) extends CommandInterpreter("heroku-helper") {

    override def StartCommandIdentifier = "abcdefghijklmnopqrstuvwxyz" +
      "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
      "0123456789.-"

    override def primaryPrompt = "-> "

    override def secondaryPrompt = "--> "

    override def handleException(e:Exception) : CommandAction = {
      logger.error(e.getMessage)
      KeepGoing
    }

    val configLoader: ConfigLoader = new TypesafeConfigConfigLoader(LocalConfigFile)
    val appsService: AppsService = new AppsServiceImpl(apiKey, Git, configLoader)

    val handlers: List[CommandHandler] = List(
      new AboutHandler,
      new ExitHandler,
      new ViewReposHandler,
      new ViewRepoHandler(appsService),
      new ViewReleasesHandler(appsService),
      new PushHandler(appsService, Shell),
      new RollbackHandler(appsService, Shell),
      new FolderInfoHandler)

  }


  val LocalConfigFile = ".heroku-helper.conf"

  val Header = {
    val raw = """
                |-------------------------------------------
                |Heroku Helper version: ${version}
                |-------------------------------------------
              """.stripMargin
    string.utils.interpolate(raw, ("version", Version.version.toString))
  }

  val apiKey: Option[String] = netrc.apiKey(netrc.DefaultPath)

  def console(apiKey: String) = {

    try{

    val cmd = new Console(apiKey)
    logger.level = Debug
    logger.info(Header)
    logger.debug("------")
    logger.debug("using apiKey: " + apiKey + " from: " + netrc.DefaultPath)
    logger.info("available commands: " + cmd.handlers.map(_.CommandName).mkString(", "))
    logger.info("run `help command` for more info")
    cmd.handleCommand(Some("folder-info"))
    cmd.mainLoop
    } catch {
      case e : Throwable => {
        logger.error("An error has occured: " + e.getMessage)
        System.exit(1)
      }
    }
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

