package org.corespring.heroku.helper.models

import com.typesafe.config.{ConfigObject, ConfigList}
import exceptions.InvalidConfigException

trait ConfigLoader {
  @throws(classOf[InvalidConfigException])
  def load: Config

  def save(config: Config)
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

import typesafe.TypesafeLoader

class TypesafeConfigConfigLoader(path: String) extends ConfigLoader with TypesafeLoader {

  import com.typesafe.config.{ConfigParseOptions, ConfigResolveOptions, ConfigFactory}
  import com.typesafe.config.{Config => TConfig}

  def load: Config = {

    try {

      import collection.JavaConverters._

      val typesafeConfig: TConfig = loadTypesafeConfig(path)
      val appTConfigs: java.util.List[_] = typesafeConfig.getConfigList("appConfigs")
      val list = appTConfigs.asScala.toList.map(_.asInstanceOf[TConfig])
      val appConfigs: List[HerokuAppConfig] = list.map(toHerokuAppConfig)
      val startupValidation = loadWithDefault((()=>typesafeConfig.getString("startupValidation")), toSome[String], None )
      new Config(startupValidation, appConfigs)

    } catch {
      case e: Throwable => throw new InvalidConfigException(e.getMessage)
    }
  }

  private def toHerokuAppConfig(typesafeConfig: TConfig): HerokuAppConfig = {
    import collection.JavaConverters._


    def toList(configList: java.util.List[_]): Seq[String] = {
      configList.asScala.toList.map(_.asInstanceOf[String])
    }

    def toPush(config: TConfig): Push = {
      new Push(
        before = loadWithDefault(() => config.getStringList("before"), toList, Seq()),
        after = loadWithDefault(() => config.getStringList("after"), toList, Seq()),
        cmd = loadWithDefault(() => config.getString("cmd"), (s: String) => s, Push.DefaultCmd)
      )
    }

    def toRollback(config: TConfig): Rollback = {
      new Rollback(
        before = loadWithDefault(() => config.getStringList("before"), toList, Seq()),
        after = loadWithDefault(() => config.getStringList("after"), toList, Seq()),
        cmd = loadWithDefault(() => config.getString("cmd"), (s: String) => s, Rollback.DefaultCmd)
      )
    }

    new HerokuAppConfig(
      gitRemoteName = typesafeConfig.getString("gitRemoteName"),
      name = typesafeConfig.getString("name"),
      push = loadWithDefault(() => typesafeConfig.getConfig("push"), toPush, new Push),
      rollback = loadWithDefault(() => typesafeConfig.getConfig("rollback"), toRollback, new Rollback)
    )
  }

  def save(config: Config) {
    throw new RuntimeException("Save not supported")
  }
}
