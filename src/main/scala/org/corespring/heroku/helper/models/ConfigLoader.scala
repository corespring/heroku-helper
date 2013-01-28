package org.corespring.heroku.helper.models

import com.typesafe.config.{ConfigObject, ConfigList}
import exceptions.InvalidConfigException

trait ConfigLoader {
  @throws(classOf[InvalidConfigException])
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

    try {

      import collection.JavaConverters._

      val configString = scala.io.Source.fromFile(path).mkString
      val typesafeConfig: TConfig = ConfigFactory.parseString(configString)
      val appTConfigs: java.util.List[_] = typesafeConfig.getConfigList("appConfigs")
      val list = appTConfigs.asScala.toList.map(_.asInstanceOf[TConfig])
      val c: List[HerokuAppConfig] = list.map(toHerokuAppConfig)
      new Config(appConfigs = c)

    } catch {
      case e: Throwable => throw new InvalidConfigException(e.getMessage)
    }
  }

  private def toHerokuAppConfig(typesafeConfig: TConfig): HerokuAppConfig = {
    import collection.JavaConverters._

    /** Load a property from the Typesafe config object.
      * If an error is thrown provide a default
      * @param dataFn - the function that returns the data and may throw an exception
      * @param convertor - the function that converts it from A => B
      * @param default - the default value
      * @tparam A - A Typesafe config type
      * @tparam B - The return type
      * @return
      */
    def load[A, B](dataFn: (() => A), convertor: (A => B), default: B): B = {
      try {
        convertor(dataFn())
      }
      catch {
        case e: Throwable => {
          default
        }
      }
    }

    def toList(configList: java.util.List[_]): Seq[String] = {
      configList.asScala.toList.map(_.asInstanceOf[String])
    }

    def toPush(config: TConfig): Push = {
      new Push(
        before = load((() => config.getStringList("before")), toList, Seq()),
        after = load((() => config.getStringList("after")), toList, Seq()),
        cmd = load((() => config.getString("cmd")), ((s: String) => s), Push.DefaultCmd)
      )
    }

    def toRollback(config: TConfig): Rollback = {
      new Rollback(
        before = load(() => config.getStringList("before"), toList, Seq()),
        after = load(() => config.getStringList("after"), toList, Seq()),
        cmd = load((() => config.getString("cmd")), ((s: String) => s), Rollback.DefaultCmd)
      )
    }

    new HerokuAppConfig(
      gitRemoteName = typesafeConfig.getString("gitRemoteName"),
      name = typesafeConfig.getString("name"),
      push = load((() => typesafeConfig.getConfig("push")), toPush, new Push),
      rollback = load((() => typesafeConfig.getConfig("rollback")), toRollback, new Rollback)
    )
  }

  def save(config: Config) {
    throw new RuntimeException("Save not supported")
  }
}
