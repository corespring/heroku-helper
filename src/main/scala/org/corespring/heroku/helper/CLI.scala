package org.corespring.heroku.helper

import grizzled.cmd._
import org.corespring.heroku.helper.handlers._
import log.{logger, Debug}
import models._
import java.io.File
import shell.{CmdResult, Shell, Git}
import scala.Some


object CLI extends App {

  val Header = {
    val raw = """
                |-------------------------------------------
                |Heroku Helper version: ${version}
                |-------------------------------------------
              """.stripMargin
    string.utils.interpolate(raw, ("version", Version.version.toString))
  }

  val LocalConfigFile = ".heroku-helper.conf"
  val LocalEnvironmentVariablesFile = ".heroku-helper-env.conf"

  val apiKey: Option[String] = netrc.apiKey(netrc.DefaultPath)

  val configLoader: ConfigLoader = new TypesafeConfigConfigLoader(LocalConfigFile)

  val environmentVariables : List[EnvironmentVariables] = new TypesafeEnvironmentVariablesLoader(LocalEnvironmentVariablesFile).load

  class Console(apiKey: String, configLoader: ConfigLoader) extends CommandInterpreter("heroku-helper") {

    override def StartCommandIdentifier = "abcdefghijklmnopqrstuvwxyz" +
      "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
      "0123456789.-"

    override def primaryPrompt = "-> "

    override def secondaryPrompt = "--> "

    override def handleException(e: Exception): CommandAction = {
      logger.error(e.getMessage)
      KeepGoing
    }

    val appsService: AppsService = new AppsServiceImpl(apiKey, Git, configLoader)

    val handlers: List[CommandHandler] = List(
      new AboutHandler,
      new ExitHandler,
      new ViewReposHandler,
      new ViewRepoHandler(appsService),
      new ViewReleasesHandler(appsService),
      new PushHandler(appsService, Shell),
      new RollbackHandler(appsService, Shell),
      new SetEnvironmentVariablesHandler(environmentVariables, Shell),
      new FolderInfoHandler)
  }

  def console(apiKey: String) = {

    try {

      val cmd = new Console(apiKey, configLoader)
      logger.level = Debug
      logger.debug("------")
      logger.debug("using apiKey: " + apiKey + " from: " + netrc.DefaultPath)
      logger.info("available commands: " + cmd.handlers.map(_.CommandName).mkString(", "))
      logger.info("run `help command` for more info")
      cmd.handleCommand(Some("folder-info"))
      cmd.mainLoop
    } catch {
      case e: Throwable => {
        logger.error("An error has occured: " + e.getMessage)
        System.exit(1)
      }
    }
  }

  def logApiKeyError = {
    logger.info(Header)
    logger.error("No api key found - have you logged in with the heroku toolbelt yet?")
  }

  def logInvalidEnvironment(validationScript: String) = logger.error("The validation script failed: " + validationScript)

  logger.info(Header)

  val validationResult: CmdResult = configLoader.load.startupValidation match {
    case Some(validationScript) => {
      Shell.run(validationScript,
        (s: String) => logger.info(s),
        (s: String) => logger.debug(s))
    }
    case _ => CmdResult.empty
  }

  validationResult match {
    case CmdResult(_, _, _, 0) => {
      apiKey match {
        case Some(key) => console(key)
        case _ => logApiKeyError
      }
    }
    case _ => logger.error("Environment validation failed: see: " + validationResult.name)
  }

}

