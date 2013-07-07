package org.corespring.heroku.helper.models


/**
 * @param startupValidation - a script to run before running the helper to validate the environment
 * @param appConfigs - helper app configs
 */
class HelperConfig(
                    val startupValidation: Option[String] = None,
                    val appConfigs: Seq[HelperAppConfig] = Seq(),
                    val logLevel: String = "info",
                    val resetEnvVars : Boolean = false) {
  def appConfigByName(name: String): Option[HelperAppConfig] = appConfigs.find(_.name == name)
}

object HelperConfig {

  def apply(startupValidation: Option[String] = None, appConfigs: Seq[HelperAppConfig] = Seq(), logLevel : String, resetEnvVars : Boolean): HelperConfig = {
    new HelperConfig(startupValidation, appConfigs, logLevel, resetEnvVars)
  }
}


case class HelperAppConfig(name: String,
                           push: Push = new Push,
                           rollback: Rollback = new Rollback)


abstract class Action(before: Seq[String] = List(),
                      after: Seq[String] = List(),
                      cmd: String)


case class Push(before: Seq[String] = Seq(),
                after: Seq[String] = Seq(),
                cmd: String = Push.DefaultCmd) extends Action(before, after, cmd) {

  def prepareCommand(gitRemote: String, branch: String): String = {
    org.corespring.heroku.helper.string.utils.interpolate(cmd,
      ("gitRemote", gitRemote),
      ("branch", branch))
  }
}

object Push {
  val DefaultCmd = "git push ${gitRemote} ${branch}:master"
}

case class Rollback(before: Seq[String] = Seq(),
                    after: Seq[String] = Seq(),
                    cmd: String = Rollback.DefaultCmd) extends Action(before, after, cmd) {

  def prepareCommand(version: String, app: String): String = {
    org.corespring.heroku.helper.string.utils.interpolate(cmd,
      ("version", version),
      ("app", app)
    )
  }
}

object Rollback {
  val DefaultCmd = "heroku releases:rollback ${version} --app ${app}"
}
