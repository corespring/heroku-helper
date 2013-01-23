package org.corespring.herokuHelper

import grizzled.cmd.{Stop, KeepGoing, CommandAction, CommandHandler}
import log.logger

package object handlers {

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
  class ExitHandler extends CommandHandler{
    val CommandName = "exit"
    val Help = "Exit Heroku Helper."

    def runCommand(commandName: String, args: String): CommandAction = {
      logger.info("Bye!")
      Stop
    }
  }

  class ViewReposHandler extends CommandHandler{
    //TODO: "view-repos" doesn't work as a command
    val CommandName = "view-repos"
    val Help = "View the heroku repos that are configured for this git folder"

    def runCommand(command:String, args: String) : CommandAction ={
      logger.info("view-repos: " + command)
      KeepGoing
    }
  }

  class PushHandler extends CommandHandler{
    val CommandName = "push"
    val Help = "push this git repository to a repo"

    def runCommand(command:String, args: String) : CommandAction ={
      logger.info("push: " + command)
      KeepGoing
    }
  }

  class RollbackHandler extends CommandHandler{
    val CommandName = "rollback"
    val Help = "rollback a heroku repo to an earlier version"

    def runCommand(command:String, args: String) : CommandAction ={
      logger.info("rollback:" + command)
      KeepGoing
    }
  }



}
