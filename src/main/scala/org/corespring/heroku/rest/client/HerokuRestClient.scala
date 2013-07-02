package org.corespring.heroku.rest.client

import org.corespring.heroku.rest.models.Release
import org.corespring.heroku.helper.string.utils
import org.corespring.heroku.rest.exceptions.HerokuRestClientException
import java.io.InputStream

object HerokuRestClient {

  private def invoke[A](request : HttpRequest, apiKey: String)(fn: (InputStream => A)): Either[HerokuRestClientException, A] = {
    try {

      import dispatch._

      url("blah")
      //Request(getFunc, appendQsHttpUrl(url), "GET")
      request
        .option(HttpOptions.connTimeout(1000))
        .option(HttpOptions.readTimeout(5000))
        .headers(("Accept", "application/vnd.heroku+json; version=3"))
        .headers(("ContentType", "application/.json"))
        .auth("", apiKey) {
        inputStream =>
          Right(fn(inputStream))
      }
    }
    catch {
      case e: HttpException => Left(new HerokuRestClientException("Error code: " + e.code + ", " + e.message))
      case t: Throwable => {
        t.printStackTrace()
        Left(new HerokuRestClientException("Unknown error: " + t.getMessage))
      }
    }
  }

  object Config {

    val url = "https://api.heroku.com/apps/${app}/config-vars"

    def get(apiKey: String, app: String): Either[HerokuRestClientException, Map[String, String]] = {
      val realUrl = utils.interpolate(url, ("app", app))
      import com.codahale.jerkson.Json._
      invoke[Map[String, String]](Http.get(realUrl), apiKey)(inputStream => parse[Map[String, String]](inputStream))
    }

    def set(apiKey:String, app:String, data : Map[String,String]) : Either[HerokuRestClientException, Map[String,String]] = {

      val json = com.codahale.jerkson.Json.generate(data)

      val postFunc: Http.HttpExec = (req,conn) => {
        conn.setDoOutput(true)
        conn.connect
        conn.getOutputStream.write(json.getBytes("utf-8"))
      }

      val request = Request(postFunc, Http.noopHttpUrl(url), "PATCH")
      import com.codahale.jerkson.Json._
      invoke[Map[String,String]](request, apiKey)(inputStream => parse[Map[String,String]](inputStream))
    }
  }


  object Releases {

    def list(apiKey: String, app: String): Either[HerokuRestClientException, List[Release]] = {
      val url = "https://api.heroku.com/apps/${app}/releases"
      val realUrl = utils.interpolate(url, ("app", app))
      import com.codahale.jerkson.Json._
      invoke(Http.get(realUrl), apiKey)(inputStream => parse[List[Release]](inputStream))
    }
  }

}