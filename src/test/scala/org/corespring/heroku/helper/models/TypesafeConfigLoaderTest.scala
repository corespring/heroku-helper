package org.corespring.heroku.helper.models

import exceptions.InvalidConfigException
import org.specs2.mutable.Specification

class TypesafeConfigLoaderTest extends Specification{

  val basePath = "src/test/resources/org/corespring/heroku/helper/models/typesafeConfigLoader"

  "TypesafeConfigLoader" should {
    "load" in {


      val typesafeLoader = new TypesafeConfigConfigLoader(basePath + "/one.conf")
      val configs =  typesafeLoader.load.appConfigs

      configs.length === 1
      val configOne = configs(0)
      configOne.push.cmd === "push_cmd"
      configOne.push.before === List("push_before_one", "push_before_two")
      configOne.push.after === List("push_after_one", "push_after_two")
      configOne.rollback.cmd === "rollback_cmd"
      configOne.rollback.before === List("rollback_before_one", "rollback_before_two")
      configOne.rollback.after === List("rollback_after_one", "rollback_after_two")
    }

    "load throw an error in" in {
      val typesafeLoader = new TypesafeConfigConfigLoader(basePath + "/noGitremote.conf")
      typesafeLoader.load must throwA[InvalidConfigException]
    }

    "load two configs " in {
      val typesafeLoader = new TypesafeConfigConfigLoader(basePath + "/two.conf")
      val configs = typesafeLoader.load.appConfigs
      configs.length === 2

      configs(0).name === "appOne"
      configs(1).name === "appTwo"

    }
  }

}

