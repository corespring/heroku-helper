package org.corespring.heroku.helper

import grizzled.cmd.{Stop, KeepGoing, CommandAction, CommandHandler}
import org.corespring.heroku.helper.models._
import shell.{CmdResult, Git, Shell}
import org.corespring.heroku.helper.log.logger
import grizzled.readline._
import scala.Some
import grizzled.readline.LineToken
import annotation.tailrec
import collection.immutable.ListSet
import com.codahale.jerkson.Json

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

    /** The shell to use */
    def shell: Shell

    /** Run a shell script
      * @throws a RuntimeException if the exitCode is not 0
      * @return
      */
    def runScript(script: String, params: String = ""): String = {
      val result: CmdResult = if (params.isEmpty) {
        shell.run(script)
      }
      else {
        shell.run(script + " " + params)
      }

      logger.debug(result.name + " code: " + result.exitCode)

      if (result.exitCode != 0) {
        throw new RuntimeException("Error running cmd: " + result.name)
      }
      result.out
    }
  }

  abstract class BaseHandler extends CommandHandler {


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
          case LineToken(s) :: Nil => headOrNil(options, context).filter(_.startsWith(s))
          case LineToken(s) :: Cursor :: Nil => headOrNil(options, context).filter(_.startsWith(s))
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


  class ViewReposHandler extends CommandHandler {
    val CommandName = "repos"
    val Help = "View the heroku repos that are configured for this git folder"

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

  class ViewRepoHandler(appsService: AppsService) extends BaseHandler {
    val CommandName = "repo"
    val Help = "View more information about a heroku repo"

    val apps = appsService.apps
    val appNames = apps.map(_.name)

    override def complete(token: String, allTokens: List[CompletionToken], line: String): List[String] = {
      completeFromOptions(token, allTokens, line, appNames)
    }

    def runCommand(command: String, args: String): CommandAction = wrap {
      () =>

        apps.find(a => a.name == args) match {
          case None => logger.info("Can't find this repo? try again")
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


  class PushHandler(appsService: AppsService, shell: Shell) extends BaseHandler with ShellRunning {
    val CommandName = "push"
    val Help = "push this git repository to a heroku remote repository"

    def shell() = shell

    /** Push can assist with the 2 params - 1st is the heroku repo, 2nd is the branch
      */
    override def complete(token: String, allTokens: List[CompletionToken], line: String): List[String] = {
      val gitRemotes = appsService.apps.map(_.gitRemote)
      val branches = appsService.branches
      completeFromOptions(token, allTokens, line, gitRemotes, branches)
    }


    def configFilename(app: HerokuApp): String = ".heroku-helper-tmp-" + app.name + ".json"

    def writeHerokuConfigToFile(app: HerokuApp): String = {
      val herokuConfig = appsService.loadHerokuConfigFor(app)
      logger.debug("herokuConfig:")
      logger.debug(herokuConfig.toString)

      val tmpDataFile = configFilename(app)
      org.corespring.file.utils.write(tmpDataFile, Json.generate(herokuConfig))
      tmpDataFile
    }


    def runCommand(command: String, args: String): CommandAction = wrap {
      () =>

        args.split(" ").toList match {
          case List() => logger.info("huh? - try again")
          case List(repo) => logger.info("add a branch")
          case List(gitRemote, branch) => {

            appsService.apps.find(_.gitRemote == gitRemote) match {
              case Some(app) => {

                if (appsService.currentRelease(app) == appsService.shortCommitHash) {
                  logger.info(app.name + " is already up to date - not pushing")
                } else {
                  appsService.loadConfigFor(app) match {
                    case Some(config) => {
                      val tmpFile = writeHerokuConfigToFile(app)
                      config.push.before.foreach(script => logger.info(runScript(script, tmpFile)))
                      val finalCmd = config.push.prepareCommand(gitRemote, branch)
                      logger.info("running push: " + finalCmd)
                      logger.info(runScript(finalCmd))
                      config.push.after.foreach(script => logger.info(runScript(script, tmpFile)))
                      //org.corespring.file.utils.delete(tmpFile)
                    }
                    case _ => logger.info("can't find a config - add one")
                  }

                }
              }
              case None => logger.info("can't find app for this remote:" + gitRemote)
            }
          }
        }
        KeepGoing
    }
  }


  class RollbackHandler(appsService: AppsService, shell: Shell) extends BaseHandler with ShellRunning {
    val CommandName = "rollback"
    val Help = "rollback a heroku repo to an earlier version"
    val ErrorMsg = "you need to specify the heroku app name and the version"

    def shell() = shell


    def runCommand(command: String, args: String): CommandAction = {

      val split = args.split(" ").toList
      split match {
        case List(appName, version) => {

          appsService.apps.find(_.name == appName) match {
            case Some(app) => {
              appsService.loadConfigFor(app) match {
                case Some(config) => {
                  appsService.releases(app).find(_.name == version) match {

                    case Some(release) => {

                      if (release.name == appsService.currentRelease(app).name) {
                        logger.info("Already at this release")
                      }
                      else {

                        def writeReleaseConfigToFile: String = {
                          val envJson = Json.generate(release.env)
                          logger.debug("env:")
                          logger.debug(envJson)
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
                        config.push.after.foreach(script => logger.info(run(script)))
                      }
                    }
                    case _ => logger.info("Can't find release information for: " + version)
                  }
                }
                case _ => logger.info("no config available - add one!")
              }
            }
            case _ => logger.info("can't find app")
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


}
