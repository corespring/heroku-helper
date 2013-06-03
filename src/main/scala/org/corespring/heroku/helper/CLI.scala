package org.corespring.heroku.helper

import com.typesafe.config.ConfigFactory
import grizzled.cmd._
import log.logger
import models._
import org.corespring.heroku.helper.handlers._
import org.corespring.heroku.helper.shell.{LoggingShell, CmdResult, Shell, Git}
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

  val config: com.typesafe.config.Config = ConfigFactory.load()
  val LocalConfigFile = ".heroku-helper.conf"
  val LocalEnvironmentVariablesFile = config.getString("envConfFile")


  val apiKey: String = netrc.apiKey(netrc.DefaultPath) match {
    case Some(key) => key
    case None => throw new RuntimeException("No api key found")
  }

  val helperConfig: HelperConfig = new TypesafeConfigConfigLoader(LocalConfigFile).load

  val environmentVariables: List[EnvironmentVariables] = try {
    new TypesafeEnvironmentVariablesLoader(LocalEnvironmentVariablesFile).load
  } catch {
    case e: Throwable => List()
  }

  val appsService: AppsService = new AppsServiceImpl(apiKey, Git, helperConfig)

  object RuntimeOptions {
    var dryRun: Boolean = false
  }

  def getShell: Shell = if (RuntimeOptions.dryRun) new LoggingShell else Shell

  val handlers: List[CommandHandler] = List(
    new AboutHandler,
    new DryRunHandler,
    new ExitHandler,
    new ViewAppsHandler,
    new InfoHandler(appsService),
    new ViewReleasesHandler(appsService),
    new PushHandler(appsService, getShell, environmentVariables),
    new RollbackHandler(appsService, Shell),
    new SetEnvironmentVariablesHandler(appsService, environmentVariables, Shell),
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
    */
  private def launchConsole {

    logger.info(Header)

    val validationResult: CmdResult = helperConfig.startupValidation match {
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

