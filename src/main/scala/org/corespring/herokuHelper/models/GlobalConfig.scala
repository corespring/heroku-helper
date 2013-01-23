package org.corespring.herokuHelper.models

import java.io.File
import org.corespring.herokuHelper.log.logger
import java.io.FileWriter

case class GlobalConfig(herokuApiKey: Option[String] = None)


object GlobalConfig {

  /** Load the config from the given file path
    */
  def fromFile(jsonPath: String): GlobalConfig = {

    val configFile: File = new File(jsonPath)

    if (configFile.exists) {
      val jsonString = scala.io.Source.fromFile(jsonPath).mkString
      com.codahale.jerkson.Json.parse[GlobalConfig](jsonString)
    }
    else {
      val newConfig = new GlobalConfig
      toFile(jsonPath, newConfig)
      newConfig
    }
  }

  def toFile(path: String, config: GlobalConfig) {
    val file: File = new File(path)

    if (!file.exists) {
      file.createNewFile()
    }

    val json = com.codahale.jerkson.Json.generate(config)

    import org.corespring.file.utils._

    write(path, json)
  }
}


