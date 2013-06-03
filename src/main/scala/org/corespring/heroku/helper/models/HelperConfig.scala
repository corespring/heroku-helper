package org.corespring.heroku.helper.models


/**
 * @param startupValidation - a script to run before running the helper to validate the environment
 * @param appConfigs - helper app configs
 * @param reservedEnvVars - a set of env vars that are reserved - they can be set but not deleted.
 */
class HelperConfig(val startupValidation: Option[String] = None, val appConfigs: Seq[HelperAppConfig] = Seq(), val reservedEnvVars: List[String] = List()) {
  def appConfigByName(name: String): Option[HelperAppConfig] = appConfigs.find(_.name == name)
}

object HelperConfig {
  val reserved: List[String] = List(
    "HEROKU_",
    "PATH",
    "JAVA_",
    "DATABASE_",
    "SBT_OPTS",
    "REPO",
    "BUILDPACK"
  )

  def apply(startupValidation: Option[String] = None, appConfigs: Seq[HelperAppConfig] = Seq(), reservedEnvVars: List[String] = List()): HelperConfig = {
    new HelperConfig(startupValidation, appConfigs, reserved ++ reservedEnvVars)
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
