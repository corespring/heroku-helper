package org.corespring.heroku.helper.models

trait ConfigLoader {
  def load: Config

  def save(config: Config): Unit
}

class FileConfigLoader(path: String) extends ConfigLoader {

  import com.codahale.jerkson.Json._
  import java.io.File

  def load: Config = {

    val configFile = new File(path)

    if (configFile.exists) {
      parse[Config](scala.io.Source.fromFile(path).mkString)
    }
    else {
      val defaultConfig = new Config
      save(defaultConfig)
      defaultConfig
    }
  }

  def save(config: Config) {
    val json = generate(config)
    import org.corespring.file.utils._
    write(path, json)
  }
}

