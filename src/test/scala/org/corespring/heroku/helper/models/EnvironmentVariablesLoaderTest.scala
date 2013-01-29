package org.corespring.heroku.helper.models

import org.specs2.mutable.Specification

class EnvironmentVariablesLoaderTest extends Specification {

  val basePath = "src/test/resources/org/corespring/heroku/helper/models/envLoader"

  "TypesafeEnvironmentVariablesLoader" should {
    "load" in {
      val loader = new TypesafeEnvironmentVariablesLoader(basePath + "/one.conf")
      loader.load.length === 2

      val firstConfig : EnvironmentVariables = loader.load(0)
      firstConfig.herokuName === "app"
      firstConfig.vars === Map("A" -> "A", "B" -> "B")

      val secondConfig : EnvironmentVariables = loader.load(1)
      secondConfig.herokuName === "app-staging"
      secondConfig.vars === Map("C" -> "C", "D" -> "D")
    }
  }

}
