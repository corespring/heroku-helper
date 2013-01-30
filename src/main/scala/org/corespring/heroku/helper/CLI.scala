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

  val apiKey: String = netrc.apiKey(netrc.DefaultPath) match {
    case Some(key) => key
    case None => throw new RuntimeException("No api key found")
  }

  val configLoader: ConfigLoader = new TypesafeConfigConfigLoader(LocalConfigFile)

  val environmentVariables: List[EnvironmentVariables] = new TypesafeEnvironmentVariablesLoader(LocalEnvironmentVariablesFile).load

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

  args.toList match {
    case Nil => launchConsole
    case command :: params => {
      handlers.find(_.CommandName == command) match {
        case Some(handler) => handler.runCommand(command, params.mkString(" "))
        case _ => launchConsole
      }
    }
  }

  /** Launch the interactive console
    *
    */
  private def launchConsole {

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
      case CmdResult(_, _, _, 0) => run
      case _ => logger.error("Environment validation failed: see: " + validationResult.name)
    }

    def run {

      try {
        val cmd = new Console(handlers)
        cmd.mainLoop
      } catch {
        case e: Throwable => {
          logger.error("An error has occured: " + e.getMessage)
          System.exit(1)
        }
      }
    }
  }
}

