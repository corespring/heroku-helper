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

class TypesafeConfigConfigLoader(path: String) extends ConfigLoader {

  import com.typesafe.config.{ConfigParseOptions, ConfigResolveOptions, ConfigFactory}
  import com.typesafe.config.{Config => TConfig}

  def load: Config = {
    //import collection.JavaConversions._
    import collection.JavaConverters._

    val parseOptions: ConfigParseOptions = ConfigParseOptions.defaults().setAllowMissing(true)
    val typesafeConfig: TConfig = ConfigFactory.load(path, parseOptions, ConfigResolveOptions.defaults())
    println(typesafeConfig)
    val appTConfigs: java.util.List[_] = typesafeConfig.getConfigList("appConfigs")
    val list = appTConfigs.asScala.toList.map(_.asInstanceOf[TConfig])
    val c: List[HerokuAppConfig] = list.map(toHerokuAppConfig)
    new Config
  }

  private def toHerokuAppConfig(typesafeConfig: TConfig): HerokuAppConfig = {
    new HerokuAppConfig(
      typesafeConfig.getString("gitRemote"),
      typesafeConfig.getString("name")
    )
  }

  def save(config: Config) {
    throw new RuntimeException("Save not supported")
  }
}
