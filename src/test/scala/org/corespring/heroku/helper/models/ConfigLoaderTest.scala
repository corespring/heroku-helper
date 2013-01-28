package org.corespring.heroku.helper.models

import org.specs2.mutable.Specification
import java.io.File

class ConfigLoaderTest extends Specification{

  "ConfigLoader" should {

    "load a config" in {

      val testPath= "src/test/resources/org/corespring/heroku/helper/models/configLoader"
      val loader = new FileConfigLoader(testPath + "/not-created.json")
      loader.load !== null
      new File(testPath + "/not-created.json").delete()
    }

    "load a config with no path" in {
      val loader = new FileConfigLoader(".not-created.json")
      loader.load !== null
      new File(".not-created.json").delete()
    }
  }

}
