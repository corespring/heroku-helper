package org.corespring.herokuHelper.models

case class Config(repos: Seq[RepoConfig] = Seq()) {

  def repo(name: String): Option[RepoConfig] = repos.find(_.name == name)
}

class ConfigLoader(path: String) {

  import com.codahale.jerkson.Json._
  import java.io.File

  def config: Config = {

    val configFile = new File(path)

    if (configFile.exists) {
      parse[Config](scala.io.Source.fromFile(path).mkString)
    }
    else {
      val defaultConfig = new Config
      saveConfig(defaultConfig)
      defaultConfig
    }
  }

  def saveConfig(config: Config) {
    val json = generate(config)
    import org.corespring.file.utils._
    write(path, json)
  }

}

case class RepoConfig(name: String,
                      push: Action = new Action,
                      rollback: Action = new Action)

case class Action(before: Seq[String] = List(),
                  after: Seq[String] = List())
