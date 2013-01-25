package org.corespring.heroku.helper.log

import grizzled.string.WordWrapper
import org.corespring.heroku.helper.exceptions.HerokuHelperException

abstract sealed class LogLevel(val value: Int) {
  def matches(s: String): Boolean = s.toLowerCase == toString
}

case object Debug extends LogLevel(40) {
  override def toString = "debug"
}

case object Verbose extends LogLevel(30) {
  override def toString = "verbose"
}

case object Info extends LogLevel(20) {
  override def toString = "info"
}

case object Warning extends LogLevel(10) {
  override def toString = "warning"
}

case object Error extends LogLevel(0) {
  override def toString = "error"
}

/**
 * Simple messaging/logging singleton.
 */
object logger {


  private var theLevel: LogLevel = Info
  var useAnsi = true

  private val wrapper = new WordWrapper(79)
  val Levels = List(Error, Warning, Info, Verbose, Debug)

  def level = theLevel

  def level_=(newLevel: Any) {
    newLevel match {
      case l: LogLevel =>
        theLevel = l

      case i: Int => {
        val l = Levels.filter(_.value == i)
        if (l == Nil)
          throw new HerokuHelperException("Bad log level: " + i)
        theLevel = l(0)
      }

      case s: String => {
        val l = Levels.filter(_.matches(s))
        if (l == Nil)
          throw new HerokuHelperException("Bad log level: " + s)
        theLevel = l(0)
      }

      case _ =>
        throw new HerokuHelperException("Bad log level: " + newLevel)
    }
  }

  /** Emit a message only if the log level is set to Debug.
    *
    * @param msg  the message
    */
  def debug(msg: => String) {
    if (level.value >= Debug.value)
      emit("[DEBUG] " + msg, Console.GREEN + Console.BOLD)
  }

  /** Emit a message only if the log level is set to Verbose or above.
    *
    * @param msg  the message
    */
  def verbose(msg: => String) {
    if (level.value >= Verbose.value)
      emit(msg, Console.BLUE + Console.BOLD)
  }

  /** Emit a message only if the log level is set to Info or above.
    *
    * @param msg  the message
    */
  def info(msg: => String) {
    if (level.value >= Info.value)
      emit(msg, "")
  }

  /** Emit a message only if the log level is set to Warning or above.
    *
    * @param msg  the message
    */
  def warning(msg: => String) {
    if (level.value >= Warning.value)
      emit("Warning: " + msg, Console.YELLOW)
  }

  /** Emit an error message. These cannot be suppressed.
    *
    * @param msg  the message
    */
  def error(msg: => String) {
    emit("Error: " + msg, Console.RED + Console.BOLD)
  }

  private def emit(msg: String, ansiModifiers: => String) {
    val wrappedMsg = wrapper.wrap(msg)
    if (useAnsi)
      println(ansiModifiers + wrappedMsg + Console.RESET)
    else
      println(wrappedMsg)
  }
}
