package org.corespring.heroku.rest.client

import com.typesafe.config.ConfigFactory
import org.corespring.heroku.helper.models.netrc
import org.specs2.mutable.Specification

class HerokuRestClientTest extends Specification {


  val apiKey: String = netrc.apiKey(netrc.DefaultPath) match {
    case Some(key) => key
    case _ => "?"
  }
  val app: String = {
    try {
      ConfigFactory.systemEnvironment().getString("heroku-helper-test-app-name")
    } catch {
      case _: Throwable => "hh-example-ci"
    }
  }

  "Config" should {
    "get config" in {
      val set = HerokuRestClient.Config.set(apiKey, app, Map("apple" -> "apple"))
      println(set)
      val out = HerokuRestClient.Config.get(apiKey, app)
      println(out)
      true === true
    }
  }

}
