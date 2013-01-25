package org.corespring.heroku.helper.models


case class Config(appConfigs: Seq[HerokuAppConfig] = Seq()) {

  def appConfigByName(name: String): Option[HerokuAppConfig] = appConfigs.find(_.name == name)

  def appConfigByGitRemote(gitRemote: String): Option[HerokuAppConfig] = appConfigs.find(_.gitRemoteName == gitRemote)
}


case class HerokuAppConfig(name: String,
                           gitRemoteName: String,
                           push: Push = new Push,
                           rollback: Rollback = new Rollback)



abstract class Action(before: Seq[String] = List(),
                      after: Seq[String] = List(),
                      cmd: String
                       )


case class Push(before: Seq[String] = Seq(),
                after: Seq[String] = Seq(),
                cmd: String = "git push ${gitRemote} ${branch}:master") extends Action(before, after, cmd) {

  def prepareCommand(gitRemote: String, branch: String): String = {
    org.corespring.heroku.helper.string.utils.interpolate(cmd,
      ("gitRemote", gitRemote),
      ("branch", branch))
  }
}

case class Rollback(before: Seq[String] = Seq(),
                    after: Seq[String] = Seq(),
                    cmd: String = "heroku releases:rollback ${version} --app ${app}") extends Action(before, after, cmd) {

  def prepareCommand(version: String, app: String): String = {
    org.corespring.heroku.helper.string.utils.interpolate(cmd,
      ("version", version),
      ("app", app)
    )
  }
}
