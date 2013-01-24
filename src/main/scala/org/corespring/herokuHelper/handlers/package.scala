package org.corespring.herokuHelper

import grizzled.cmd.{Stop, KeepGoing, CommandAction, CommandHandler}
import log.logger
import models.{ConfigLoader, RepoConfig}
import shell.git.GitInfo
import shell.{Git, Shell}
import sun.security.krb5.Config
import org.corespring.heroku.client.HerokuRestClient
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

  class ViewReleasesHandler(apiKey:String, loader:ConfigLoader) extends CommandHandler{
    val CommandName = "releases"
    val Help = "View all releases for a repo"


    override def complete(token:String, allTokens : List[CompletionToken], line : String ) : List[String] = {
      Git.repos.map( tuple => tuple._2)
    }

    def runCommand(command:String, args:String) : CommandAction = wrap {
      () =>

        Git.repos.find(tuple => tuple._2 == args) match {
          case None => logger.info("Repo not found")
          case Some((remoteName,herokuName)) => {
            HerokuRestClient.Releases.list(apiKey,herokuName) match {
              case Left(error) => logger.error("no releases found")
              case Right(releases) => {
                val releasesString = releases.map(r => "name: " + r.name + ", commit: " + r.commit + ", date: " + r.created_at)
                logger.info(releasesString.mkString("\n"))
              }
            }
          }
        }
    }
  }

  class ViewRepoHandler(apiKey: String, loader: ConfigLoader) extends CommandHandler {
    val CommandName = "repo"
    val Help = "View more information about a heroku repo"

    /**
     *
     * @param token - the string that has been entered
     * @param allTokens - all that has been entered
     * @param line - string of whats completed so far?
     * @return
     */
    override def complete(token:String, allTokens : List[CompletionToken], line : String ) : List[String] = {
      Git.repos.map( tuple => tuple._2)
    }

    def runCommand(command: String, args: String): CommandAction = wrap {
      () =>

        Git.repos.find(tuple => tuple._2 == args) match {
          case None => logger.info("Can't find this repo? try again")
          case Some((remoteName, herokuName)) => {
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

                HerokuRestClient.Releases.list(apiKey, herokuName) match {
                  case Right(releases) => {
                    logger.info(HRule)
                    logger.info("Current Heroku Release")
                    logger.info(HRule)
                    logger.info("Name: " + releases.last.name)
                    logger.info("Commit hash: " + releases.last.commit)
                    logger.info("Created: " + releases.last.created_at)
                    logger.info("User: " + releases.last.user)
                    logger.info("Environment variables")
                    releases.last.env.foreach( kv => logger.info(kv._1 + ": " + kv._2))
                  }
                  case Left(error) => logger.error("couldn't load release info from heroku")
                }
              }
              case _ => logger.info("no config found - add one in " + CLI.LocalConfigFile)
            }
          }
        }
    }
  }


  class PushHandler(gitInfo:GitInfo) extends CommandHandler{
    val CommandName = "push"
    val Help = "push this git repository to a heroku remote repository"

    override def complete(token:String, allTokens : List[CompletionToken], line : String ) : List[String] = {

      val repos = gitInfo.repos.map( tuple => tuple._2)
      allTokens match {
        case LineToken(CommandName) :: Delim :: Cursor :: Nil => repos
        case LineToken(CommandName) :: Delim :: LineToken(str) :: Cursor :: Nil => repos.filter(_.startsWith(str))
        case LineToken(CommandName) :: Delim :: LineToken(repo) :: Delim :: Cursor :: Nil => gitInfo.branches
        case LineToken(CommandName) :: Delim :: LineToken(repo) :: Delim :: LineToken(br) :: Cursor :: Nil => gitInfo.branches.filter(_.startsWith(br))
        case _ => List()
      }
    }

    def runCommand(command: String, args: String): CommandAction = wrap {
      () =>
      logger.info("push: " + command + " args: " + args)
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
