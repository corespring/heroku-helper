package org.corespring.heroku.helper.models

import org.specs2.mutable.Specification

class TypesafeConfigLoaderTest extends Specification{

  val basePath = "src/test/resources/org/corespring/heroku/helper/models/typesafeConfigLoader"

  "TypesafeConfigLoader" should {
    "load" in {

      val typesafeLoader = new TypesafeConfigConfigLoader(basePath + "/one.conf")

      typesafeLoader.load.appConfigs.length === 1
    }
  }

}

