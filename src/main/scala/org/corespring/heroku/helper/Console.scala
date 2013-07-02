package org.corespring.heroku.helper

import grizzled.cmd.{KeepGoing, CommandAction, CommandHandler, CommandInterpreter}
import log.{logger, Debug}


class Console( injectedHandlers : List[CommandHandler]) extends CommandInterpreter("heroku-helper") {

  override def StartCommandIdentifier = "abcdefghijklmnopqrstuvwxyz" +
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
    "0123456789.-"

  override def primaryPrompt = "-> "

  override def secondaryPrompt = "--> "

  override def handleException(e: Exception): CommandAction = {
    logger.error(e.getMessage)
    KeepGoing
  }

  val handlers : List[CommandHandler] = injectedHandlers
  logger.level = Debug
  logger.info("available commands: " + handlers.map(_.CommandName).mkString(", "))
  logger.info("run `help command` for more info")
}
