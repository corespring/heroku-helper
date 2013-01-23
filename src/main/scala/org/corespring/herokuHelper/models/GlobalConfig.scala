package org.corespring.herokuHelper.models

import java.io.File
import org.corespring.herokuHelper.log.logger
import java.io.FileWriter

case class GlobalConfig(herokuApiKey: Option[String] = None)


object GlobalConfig {
  def fromFile(jsonPath: String): GlobalConfig = {

    val configFile: File = new File(jsonPath)

    if (configFile.exists) {
      val jsonString = scala.io.Source.fromFile(jsonPath).mkString
      com.codahale.jerkson.Json.parse[GlobalConfig](jsonString)
    }
    else {
      logger.info("doesn't exist")
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

    def writeToFile(path: String, s: String): File = {
      val fw = new FileWriter(path)
      fw.write(s)
      fw.close()
      new File(path)
    }

    val json = com.codahale.jerkson.Json.generate(config)
    writeToFile(path, json)
  }
}


