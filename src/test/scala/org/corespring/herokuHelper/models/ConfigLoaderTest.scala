package org.corespring.herokuHelper.models

import org.specs2.mutable.Specification
import java.io.File

class ConfigLoaderTest extends Specification{

  "ConfigLoader" should {

    "load a config" in {

      val testPath= "src/test/resources/org/corespring/herokuHelper/models/configLoader"
      val loader = new ConfigLoader(testPath + "/not-created.json")
      loader.config !== null
      new File(testPath + "/not-created.json").delete()
    }

    "load a config with no path" in {
      val loader = new ConfigLoader(".not-created.json")
      loader.config !== null
      new File(".not-created.json").delete()
    }
  }

}
