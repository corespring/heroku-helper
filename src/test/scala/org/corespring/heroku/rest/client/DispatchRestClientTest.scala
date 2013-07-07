package org.corespring.heroku.rest.client

import com.typesafe.config.ConfigFactory
import org.corespring.heroku.helper.models.netrc
import org.specs2.mutable.Specification

class DispatchRestClientTest extends Specification {


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

  val defaultMap = Map("PATH" -> "bin:node_modules/.bin:/usr/local/bin:/usr/bin:/bin")

  val client = DispatchRestClient
  "Config" should {

    "set and get config" in {
      val m = Map("COLOR" -> "Blue")
      client.config.set(apiKey, app, m) === Right(defaultMap ++ m)
      client.config.get(apiKey, app) === Right(defaultMap ++ m)
      val empty: Map[String, String] = Map()
      client.config.set(apiKey, app, empty) === Right(defaultMap ++ m)
      client.config.get(apiKey, app) === Right(defaultMap ++ m)
      client.config.unset(apiKey,app, Seq("COLOR"))
      client.config.get(apiKey, app) === Right(defaultMap)
    }
  }

  "Releases" should {
    "return releases" in {
      client.releases.list(apiKey, app) match {
        case Left(e) => failure(e.getMessage)
        case Right(r) => r.length > 0
      }
    }
  }

}
