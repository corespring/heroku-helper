package org.corespring.heroku.helper

import annotation.tailrec
import com.codahale.jerkson.Json
import grizzled.cmd.{Stop, KeepGoing, CommandAction, CommandHandler}
import grizzled.readline._
import org.corespring.heroku.helper.log.logger
import org.corespring.heroku.helper.models._
import org.corespring.heroku.rest.models.Release
import scala.Some
import shell.{CmdResult, Git, Shell}
import org.corespring.heroku.helper.CLI.RuntimeOptions

package object handlers {

  val HRule: String = "--------------------------------------"

  /** wrap simple commands with a little bit of formatting
    * @param body
    * @return
    */
  private def wrap(body: (() => Unit)): CommandAction = {
    logger.info(HRule)
    body()
    logger.info(HRule)
    KeepGoing
  }

  trait ShellRunning {

    def out(s: String) {
      logger.info(s)
    }

    def err(s: String) {
      logger.error(s)
    }

    /** The shell to use */
    def shell: Shell

    /** Run a shell script
      * @throws a RuntimeException if the exitCode is not 0
      * @return
      */
    def runScript(script: String, params: String = ""): String = {
      val result: CmdResult = if (params.isEmpty) {
        shell.run(script, out, err)
      }
      else {
        shell.run(script + " " + params, out, err)
      }
      if (result.exitCode != 0) {
        throw new RuntimeException("Error running cmd: " + result.name)
      }
      result.out
    }
  }

  trait AppsHelper {
    def service: AppsService

    /** A helper method that looks up an app and a dependent object
      */
    def withApp[A](name: String,
                   appConverter: (HerokuApp => Option[A]))
                  (body: ((HerokuApp, A) => Unit)) {
      service.apps.find(_.name == name) match {
        case Some(app) => {
          appConverter(app) match {
            case Some(thing) => body(app, thing)
            case _ => logger.info("HerokuHelper:: can't convert app to desired object: " + name)
          }
        }
        case _ => {
          logger.info("can't find app with name: " + name)
        }
      }
    }

  }

  abstract class BaseHandler extends CommandHandler {

    /** Default input matching behaviour
      * Matches the input string as an ordered sequence within the source string
      */
    protected def inputMatches(source: String, input: String): Boolean = {
      import scala.util.matching._
val regex: Regex = new Regex("(.*" + input.split("").toList.mkString(".*?") + ".*)", "all")
      regex.findFirstIn(source).isDefined
    }

    /** A completion helper that digs through the tokens calling the appropriate contextual function
      *
      * @param token
      * @param allTokens
      * @param line
      * @param contextualFns - a function that takes the context command (the preceding word) and returns a List[String]
      * @return
      */
    def completeContextually(token: String,
                             allTokens: List[CompletionToken],
                             line: String,
                             contextualFns: (String => List[String])*): List[String] = {

      def headOrNil(l: List[(String => List[String])], context: String) = l match {
        case Nil => Nil
        case _ => l.head(context)
      }


      @tailrec
      def completeRecursively(context: String, tokens: List[CompletionToken], options: List[(String => List[String])]): List[String] = {
        tokens match {
          case Cursor :: Nil => headOrNil(options, context)
          case LineToken(s) :: Nil => headOrNil(options, context).filter(inputMatches(_, s))
          case LineToken(s) :: Cursor :: Nil => headOrNil(options, context).filter(inputMatches(_, s))
          case LineToken(s) :: Delim :: rest => completeRecursively(s, rest, options.tail)
          case List() => List()
          case _ => List()
        }
      }

      allTokens match {
        case LineToken(CommandName) :: Delim :: rest => completeRecursively(CommandName, rest, contextualFns.toList)
        case _ => List()
      }

    }

    /** A convenience method when you want to complete base on a known list set that
      * doesn't depend on context.
      */
    def completeFromOptions(token: String,
                            allTokens: List[CompletionToken],
                            line: String,
                            options: List[String]*): List[String] = {

      val asContextFunctions = options.map(o => ((s: String) => o))
      completeContextually(token, allTokens, line, asContextFunctions: _*)
    }
  }



  class DryRunHandler extends CommandHandler{
    val CommandName = "dry-run"
    val Help = "Toggle dry run"

    def runCommand(command:String, args : String) : CommandAction = {
      RuntimeOptions.dryRun = args.startsWith("true")
      logger.info("dry run: " + RuntimeOptions.dryRun)
      KeepGoing
    }
  }
  /** Handler the "about" command
    */
  class AboutHandler extends CommandHandler {
    val CommandName: String = "about"
    val Help: String = "Tells you a little about this example"

    def runCommand(command: String, unparsedArgs: String): CommandAction = {
      logger.info("Heroku helper is a command line utility for working with your heroku deployements")
      logger.info("more info here: https://github.com/corespring/heroku-helper")
      KeepGoing
    }
  }

  /** Handles the "exit" command
    */
  class ExitHandler extends CommandHandler {
    val CommandName = "exit"
    val Help = "Exit Heroku Helper."

    def runCommand(commandName: String, args: String): CommandAction = {
      logger.info("Bye!")
      Stop
    }
  }


  class ViewAppsHandler extends CommandHandler {
    val CommandName = "apps"
    val Help = "View the heroku apps that are configured for this git folder"

    def runCommand(command: String, args: String): CommandAction = wrap {
      () =>
        val repos = Git.repos.map(tuple => tuple._2)
        logger.info(repos.mkString("\n"))
    }
  }

  class ViewReleasesHandler(appsService: AppsService) extends BaseHandler {
    val CommandName = "releases"
    val Help = "View all releases for a repo"

    override def complete(token: String, allTokens: List[CompletionToken], line: String): List[String] = {
      completeFromOptions(token, allTokens, line, appsService.apps.map(_.name))
    }

    def runCommand(command: String, args: String): CommandAction = wrap {
      () =>

        appsService.apps.find(_.name == args) match {
          case None => logger.info("Repo not found")
          case Some(herokuApp) => {
            val pretty = appsService.releases(herokuApp).map(PrettyPrint.release)
            logger.info(pretty.mkString("\n"))
          }
        }
    }
  }

  class InfoHandler(appsService: AppsService) extends BaseHandler {
    val CommandName = "info"
    val Help = "View more information about a heroku application"

    val apps = appsService.apps
    val appNames = apps.map(_.name)

    override def complete(token: String, allTokens: List[CompletionToken], line: String): List[String] = {
      completeFromOptions(token, allTokens, line, appNames)
    }

    def runCommand(command: String, args: String): CommandAction = wrap {
      () =>

        apps.find(a => a.name == args) match {
          case None => logger.info("Can't find this app? try again")
          case Some(app) => {

            appsService.loadConfigFor(app) match {
              case Some(repoConfig) => {
                logger.info(PrettyPrint.config(repoConfig))
                val currentRelease = appsService.currentRelease(app)
                logger.info(PrettyPrint.release(currentRelease))
              }
              case _ => logger.info("no config found - add one in " + CLI.LocalConfigFile)
            }
          }
        }
    }
  }


  class PushHandler(appsService: AppsService, shellLoader: => Shell, envVars: List[EnvironmentVariables])
    extends BaseHandler
    with ShellRunning
    with AppsHelper
    with EnvVarSetting {
    val CommandName = "push"
    val Help = "push this git repository to a heroku remote repository"

    lazy val shell : Shell = shellLoader

    def service() = appsService

    /** Push can assist with the 2 params - 1st is the heroku repo, 2nd is the branch
      */
    override def complete(token: String, allTokens: List[CompletionToken], line: String): List[String] = {
      val appNames = appsService.apps.map(_.name)
      val branches = appsService.branches
      completeFromOptions(token, allTokens, line, appNames, branches)
    }


    def configFilename(app: HerokuApp): String = ".heroku-helper-tmp-" + app.name + ".json"

    def writeHerokuConfigToFile(app: HerokuApp): String = {
      val herokuConfig = appsService.loadHerokuConfigVars(app)
      val json = Json.generate(herokuConfig)
      if(RuntimeOptions.dryRun){
        logger.info(json)
      }
      val tmpDataFile = configFilename(app)
      org.corespring.file.utils.write(tmpDataFile, json)
      tmpDataFile
    }

    def envVarsToClear(app: HerokuApp): List[String] = {
      def isReserved(key: String): Boolean = {
        appsService.reservedEnvVars.exists(key.startsWith(_))
      }

      val currentConfigVars = appsService.loadHerokuConfigVars(app)
      currentConfigVars.toList.map(_._1).filterNot(isReserved)
    }


    def runCommand(command: String, args: String): CommandAction = wrap {
      () =>

        println("args: " + args)
        def isCurrentRelease(app: HerokuApp): Boolean = {
          val currentRelease: Release = appsService.currentRelease(app)
          logger.debug("current release: " + currentRelease)
          logger.debug("short commit hash: " + appsService.shortCommitHash)
          currentRelease.commit == appsService.shortCommitHash
        }

        args.split(" ").toList match {
          case List(appName, branch) => {
            withApp(appName, (app) => appsService.loadConfigFor(app)) {
              (app: HerokuApp, config: HelperAppConfig) =>

                if (isCurrentRelease(app)) {
                  logger.info(app.name + " is already up to date - not pushing")
                } else {

                  clearEnvVars(app.name, envVarsToClear(app))
                  setEnvVarsForApp(app.name, envVars)

                  val tmpFile = writeHerokuConfigToFile(app)
                  config.push.before.foreach(script => {
                    println("[heroku-helper] run: " + script)
                    logger.info("run: " + script)
                    logger.info(runScript(script, tmpFile + " " + app.name + " " + branch))
                  })
                  val finalCmd = config.push.prepareCommand(app.gitRemote, branch)
                  logger.info("running push: " + finalCmd)
                  logger.info(runScript(finalCmd))
                  config.push.after.foreach(script => {
                    logger.info("run: " + script)
                    logger.info(runScript(script, tmpFile + " " + app.name + " " + branch))
                  })
                }
            }
          }
          case _ => logger.info("try again tou need to specify an app and a branch")
        }

        KeepGoing
    }
  }


  class RollbackHandler(appsService: AppsService, shell: Shell)
    extends BaseHandler
    with ShellRunning with AppsHelper {
    val CommandName = "rollback"
    val Help = "rollback a heroku repo to an earlier version"
    val ErrorMsg = "you need to specify the heroku app name and the version"

    def shell() = shell

    def service() = appsService


    def runCommand(command: String, args: String): CommandAction = {

      val split = args.split(" ").toList
      split match {
        case List(appName, version) => {

          withApp(appName, (app) => appsService.loadConfigFor(app)) {

            (app: HerokuApp, config: HelperAppConfig) =>

              for {
                release <- appsService.releases(app).find(_.name == version)
                if (release.name != appsService.currentRelease(app).name)
              } yield {

                def writeReleaseConfigToFile: String = {
                  val envJson = Json.generate(release.env)
                  val filename = ".rollback-" + release.name
                  org.corespring.file.utils.write(filename, envJson)
                  filename
                }

                val tmpFile = writeReleaseConfigToFile

                def run(script: String): String = runScript(script, release.commit + " " + tmpFile)

                config.rollback.before.foreach(script => logger.info(run(script)))
                val finalCmd = config.rollback.prepareCommand(version, appName)
                logger.debug("running rollback: " + finalCmd)
                logger.info(runScript(finalCmd))
                config.rollback.after.foreach(script => logger.info(run(script)))
              }

          }
        }
        case _ => logger.info(ErrorMsg)
      }
      KeepGoing
    }

    override def complete(token: String, allTokens: List[CompletionToken], line: String): List[String] = {

      val appNames = appsService.apps.map(_.name)

      def releaseNames(appName: String): List[String] = appsService.apps.find(_.name == appName) match {
        case Some(app) => appsService.releases(app).map(_.name)
        case None => List()
      }

      completeContextually(token, allTokens, line, (command: String) => appNames, releaseNames)
    }
  }

  class FolderInfoHandler extends CommandHandler {
    val CommandName = "folder-info"
    val Help = "show information about this folder"

    def runCommand(command: String, args: String): CommandAction = wrap {
      () =>
        logger.info("directory: " + Shell.run("pwd"))
        logger.info("commit hash: " + Shell.run("git rev-parse --short HEAD"))
    }
  }


  class SetEnvironmentVariablesHandler(appsService: AppsService, environmentVariables: List[EnvironmentVariables], shell: Shell)
    extends BaseHandler with ShellRunning
    with EnvVarSetting {
    val CommandName = "set-env-vars"
    val Help = "set env vars based on what it is configured in .heroku-helper-env.conf (note: always happens when you do a push)"

    def shell() = shell

    def runCommand(command: String, args: String): CommandAction = wrap {
      () =>
        args.split(" ").toList match {
          case List(appName) => setEnvVarsForApp(appName, environmentVariables)
          case _ => logger.info("try again - you need to specify an app name")
        }
    }


    override def complete(token: String, allTokens: List[CompletionToken], line: String): List[String] = {
      val appNames = appsService.apps.map(_.name)
      completeFromOptions(token, allTokens, line, appNames)
    }
  }


  trait EnvVarSetting {
    self: ShellRunning =>

    def clearEnvVars(appName: String, vars: List[String]) {
      if (vars.length > 0) {
        val cmd = "heroku config:remove " + vars.mkString(" ") + " --app " + appName
        shell.run(cmd, out, err)
      }
    }

    def setEnvVarsForApp(appName: String, envVars: List[EnvironmentVariables]) {
      envVars.find(_.herokuName == appName) match {
        case Some(ev) => {
          logger.debug("Setting env vars for app: " + appName + " vars: \n" + envVars.mkString("\n"))
          runConfigSet(ev)
        }
        case _ => logger.info("can't find environment variables for: " + appName)
      }
    }

    private def runConfigSet(ev: EnvironmentVariables) {
      def toCmd(kv: (String, String)): String = kv._1 + "=" + kv._2
      val cmd: String = "heroku config:set " + ev.vars.map(toCmd).mkString(" ") + " --app " + ev.herokuName
      logger.debug("running: " + cmd)
      shell.run(cmd, out, err)
    }
  }

}
