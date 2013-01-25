package org.corespring.heroku.helper.models

import org.specs2.mutable.Specification

class GlobalConfigTest extends Specification {

  val testPath = "src/test/resources/org/corespring/heroku/helper/models/globalConfig"
  "GlobalConfig" should {

    "load from a json file" in {
      val config = GlobalConfig.fromFile(testPath + "/config.json")
      config.herokuApiKey === Some("my-key")
    }

    "creates a default config if the file doesn't exist" in {

      val config = GlobalConfig.fromFile(testPath + "/tmp.json")
      config.herokuApiKey === None
      scala.io.Source.fromFile(testPath + "/tmp.json").mkString === "{}"

      //Tidy up
      import java.io.File
      val tmpFile : File = new File(testPath + "/tmp.json")
      tmpFile.delete()
    }
  }

}
