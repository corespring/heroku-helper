package org.corespring.heroku.helper

import grizzled.cmd.{Stop, KeepGoing, CommandAction, CommandHandler}
import org.corespring.heroku.helper.models._
import org.corespring.heroku.helper.shell.git.GitInfo
import org.corespring.heroku.helper.shell.{Git, Shell}
import org.corespring.heroku.helper.log.logger
import org.corespring.heroku.rest.client.HerokuRestClient
import grizzled.readline._
import scala.Left
import scala.Some
import scala.Right
import grizzled.readline.LineToken

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

  class ViewReleasesHandler(appsService: AppsService) extends CommandHandler {
    val CommandName = "releases"
    val Help = "View all releases for a repo"


    override def complete(token: String, allTokens: List[CompletionToken], line: String): List[String] = {
      appsService.apps.map(_.name)
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

  class ViewRepoHandler(appsService: AppsService) extends CommandHandler {
    val CommandName = "repo"
    val Help = "View more information about a heroku repo"

    val apps = appsService.apps
    val appNames = apps.map(_.name)

    override def complete(token: String, allTokens: List[CompletionToken], line: String): List[String] = {

      allTokens match {
        case LineToken(CommandName) :: rest => rest match {
          case Delim :: Cursor :: Nil => appNames
          case Delim :: LineToken(str) :: Cursor :: Nil => appNames.filter(_.startsWith(str))
          case _ => List()
        }
        case _ => List()
      }
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


  class PushHandler(appsService: AppsService, shell: Shell) extends CommandHandler {
    val CommandName = "push"
    val Help = "push this git repository to a heroku remote repository"

    /** Push can assist with the 2 params - 1st is the heroku repo, 2nd is the branch
      */
    override def complete(token: String, allTokens: List[CompletionToken], line: String): List[String] = {

      val gitRemotes = appsService.apps.map(_.gitRemote)
      val branches = appsService.branches
      allTokens match {
        case LineToken(CommandName) :: rest => rest match {
          case Delim :: Cursor :: Nil => gitRemotes
          case Delim :: LineToken(str) :: Cursor :: Nil => gitRemotes.filter(_.startsWith(str))
          case Delim :: LineToken(repo) :: Delim :: Cursor :: Nil => branches
          case Delim :: LineToken(repo) :: Delim :: LineToken(br) :: Cursor :: Nil => branches.filter(_.startsWith(br))
          case _ => List()
        }
        case _ => List()
      }
    }

    def runCommand(command: String, args: String): CommandAction = wrap {
      () =>

        args.split(" ").toList match {
          case List() => logger.info("huh? - try again")
          case List(repo) => logger.info("add a branch")
          case List(gitRemote, branch) => {

            appsService.apps.find(_.gitRemote == gitRemote) match {
              case Some(app) => {
                appsService.loadConfigFor(app) match {
                  case Some(config) => {
                    config.push.before.foreach(script => logger.info(shell.run(script)))
                    val finalCmd = config.push.prepareCommand(gitRemote, branch)
                    logger.info("running push: " + finalCmd)
                    logger.info(shell.run(finalCmd))
                    config.push.after.foreach(script => logger.info(shell.run(script)))
                  }
                  case _ => logger.info("can't find a config - add one")
                }
              }
              case None => logger.info("can't find app for this remote:" + gitRemote)
            }
          }
        }
        KeepGoing
    }
  }


  class RollbackHandler(appsService: AppsService, shell: Shell) extends CommandHandler {
    val CommandName = "rollback"
    val Help = "rollback a heroku repo to an earlier version"
    val ErrorMsg = "you need to specify the heroku app name and the version"

    def runCommand(command: String, args: String): CommandAction = {

      val split = args.split(" ").toList
      split match {
        case List(appName, version) => {

          appsService.apps.find(_.name == appName) match {
            case Some(app) => {
              appsService.loadConfigFor(app) match {
                case Some(config) => {
                  config.rollback.before.foreach(script => logger.info(shell.run(script)))
                  val finalCmd = config.rollback.prepareCommand(version,appName)
                  logger.debug("running push: " + finalCmd)
                  logger.info(shell.run(finalCmd))
                  config.push.after.foreach(script => logger.info(shell.run(script)))
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

    /** Push can assist with the 2 params - 1st is the heroku repo, 2nd is the branch
      */
    override def complete(token: String, allTokens: List[CompletionToken], line: String): List[String] = {

      val appNames = appsService.apps.map(_.name)

      allTokens match {
        case LineToken(CommandName) :: rest => rest match {
          case Delim :: Cursor :: Nil => appNames
          case Delim :: LineToken(str) :: Cursor :: Nil => appNames.filter(_.startsWith(str))
          case Delim :: LineToken(appName) :: Delim :: Cursor :: Nil => {
            appsService.apps.find(_.name == appName) match {
              case Some(app) => appsService.releases(app).map(_.name)
              case None => List()
            }
          }
          case Delim :: LineToken(appName) :: Delim :: LineToken(version) :: Cursor :: Nil => {
            appsService.apps.find(_.name == appName) match {
              case Some(app) => appsService.releases(app).map(_.name).filter(_.startsWith(version))
              case None => List()
            }

          }
          case _ => List()
        }
        case _ => List()
      }
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
