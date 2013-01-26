package org.corespring.heroku.rest.client

import org.corespring.heroku.rest.models.Release
import scalaj.http.{HttpException, HttpOptions, Http}
import org.corespring.heroku.helper.string.utils
import org.corespring.heroku.rest.exceptions.HerokuRestClientException
import java.io.InputStream

object HerokuRestClient {

  def invoke[A](url: String, apiKey: String)(fn: (InputStream => A)): Either[HerokuRestClientException, A] = {
    try {
      Http.get(url)
        .option(HttpOptions.connTimeout(1000))
        .option(HttpOptions.readTimeout(5000))
        .headers(("Accept", "application/json"))
        .auth("", apiKey) {
        inputStream =>
          Right(fn(inputStream))
      }
    }
    catch {
      case e: HttpException => Left(new HerokuRestClientException("Error code: " + e.code + ", " + e.message))
      case t: Throwable => Left(new HerokuRestClientException("Unkown error: " + t.getMessage))
    }
  }

  object Config {

    def config(apiKey: String, app: String): Either[HerokuRestClientException, Map[String, String]] = {
      val url = "https://api.heroku.com/apps/${app}/config_vars"
      val realUrl = utils.interpolate(url, ("app", app))
      import com.codahale.jerkson.Json._
      invoke[Map[String, String]](realUrl, apiKey)(inputStream => parse[Map[String, String]](inputStream))
    }
  }


  object Releases {

    def list(apiKey: String, app: String): Either[HerokuRestClientException, List[Release]] = {
      val url = "https://api.heroku.com/apps/${app}/releases"
      val realUrl = utils.interpolate(url, ("app", app))
      import com.codahale.jerkson.Json._
      invoke(realUrl, apiKey)(inputStream => parse[List[Release]](inputStream))
    }
  }

}