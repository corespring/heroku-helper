package org.corespring.herokuHelper

import grizzled.cmd.{Stop, KeepGoing, CommandAction, CommandHandler}
import log.logger
import models.{ConfigLoader, RepoConfig}
import shell.{Git, Shell}
import sun.security.krb5.Config

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


  class ViewRepoHandler(loader: ConfigLoader) extends CommandHandler {
    val CommandName = "repo"
    val Help = "View more information about a heroku repo"

    def runCommand(command: String, args: String): CommandAction = wrap {
      () =>

        Git.repos.find(tuple => tuple._2 == args) match {
          case None => logger.info("Can't find this repo? try again")
          case Some((remoteName,herokuName)) => {
            loader.config.repo(herokuName) match {
              case Some(repoConfig) => {
                logger.info("pre-push scripts:")
                logger.info(repoConfig.push.before.mkString("\n"))
                logger.info("post-push scripts:")
                logger.info(repoConfig.push.after.mkString("\n"))
                logger.info("pre-rollback scripts:")
                logger.info(repoConfig.rollback.before.mkString("\n"))
                logger.info("post-rollback scripts:")
                logger.info(repoConfig.rollback.after.mkString("\n"))
              }
              case _ => logger.info("no config found - add one in " + CLI.LocalConfigFile)
            }
          }
        }
    }
  }

  class PushHandler extends CommandHandler {
    val CommandName = "push"
    val Help = "push this git repository to a repo"

    def runCommand(command: String, args: String): CommandAction = {
      logger.info("push: " + command)
      KeepGoing
    }
  }

  class RollbackHandler extends CommandHandler {
    val CommandName = "rollback"
    val Help = "rollback a heroku repo to an earlier version"

    def runCommand(command: String, args: String): CommandAction = {
      logger.info("rollback:" + command)
      KeepGoing
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
