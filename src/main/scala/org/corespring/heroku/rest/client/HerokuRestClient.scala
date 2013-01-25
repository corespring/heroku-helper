package org.corespring.heroku.rest.client

import org.corespring.heroku.rest.models.Release
import scalaj.http.{HttpException, HttpOptions, Http}
import org.corespring.heroku.helper.string.utils
import org.corespring.heroku.rest.exceptions.HerokuRestClientException

object HerokuRestClient {

  object Releases {

    def list(apiKey: String, app: String): Either[HerokuRestClientException, List[Release]] = {

      val url = "https://api.heroku.com/apps/${app}/releases"

      val realUrl = utils.interpolate(url, ("app",app) )

      try {
        Http.get(realUrl)
          .option(HttpOptions.connTimeout(1000))
          .option(HttpOptions.readTimeout(5000))
          .headers(("Accept", "application/json"))
          .auth("", apiKey) {
          inputStream =>
            val output = com.codahale.jerkson.Json.parse[List[Release]](inputStream)
            Right(output)
        }
      }
      catch {
        case e: HttpException => Left(new HerokuRestClientException("Error code: " + e.code + ", " + e.message))
        case t: Throwable => Left(new HerokuRestClientException("Unkown error: " + t.getMessage))
      }
    }

  }

}