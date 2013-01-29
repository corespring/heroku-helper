package org.corespring.heroku.helper.models

import com.typesafe.config.{ConfigValue, ConfigFactory}

import org.corespring.heroku.helper.log
import org.corespring.heroku.helper.log.{logger, Debug}
import org.corespring.heroku.helper.log.{Debug, logger}

trait EnvironmentVariablesLoader {

  def load: List[EnvironmentVariables]
}

import typesafe.TypesafeLoader

class TypesafeEnvironmentVariablesLoader(file: String) extends EnvironmentVariablesLoader with TypesafeLoader {

  import com.typesafe.config.{Config => TConfig}

  def load: List[EnvironmentVariables] = {

    try {
      import collection.JavaConverters._
      val config: TConfig = loadTypesafeConfig(file)
      val rawEnvironments : java.util.List[_] = config.getConfigList("environments")
      val list = rawEnvironments.asScala.toList.map(_.asInstanceOf[TConfig])
      list.map(toEnvironmentVariables)
    }
    catch {
      case e : Throwable => {
        logger.error(e.getMessage)
        List()
      }
    }
  }

  private def toEnvironmentVariables(config: TConfig) = {
     EnvironmentVariables(
       herokuName = config.getString("name"),
       vars = loadWithDefault( () => config.getConfig("vars"), toMap, Map())
     )
  }

  private def toMap(config: TConfig) : Map[String,String] = {
    import collection.JavaConverters._
    val set : List[(String,String)] = config.entrySet().asScala.toList.map{ e:java.util.Map.Entry[String,ConfigValue] =>
      println(e)
      (e.getKey, e.getValue.unwrapped.toString)
    }
    set.toMap
  }
}
