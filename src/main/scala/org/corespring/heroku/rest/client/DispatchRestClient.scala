package org.corespring.heroku.rest.client

import com.ning.http.client.RequestBuilder
import dispatch._
import org.corespring.heroku.rest.exceptions.HerokuRestClientException
import org.corespring.heroku.rest.models.unsupported.Release
import org.json4s._
import org.json4s.native.JsonMethods._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scalaz._
import Scalaz._

object DispatchRestClient extends HerokuRestClient {

  val ReservedKeys = List("PATH")

  val base = "https://api.heroku.com/apps"

  implicit val formats = DefaultFormats


  private def request(apiKey: String, u:String, version : Option[Int] = Some(3)): RequestBuilder = {

    url(u)
      .addHeader("Accept", version.map( v => "application/vnd.heroku+json; version=" + 3).getOrElse("application/json"))
      .addHeader("ContentType", "application/.json")
      .as_!("", apiKey)
  }

  def parseString(s: String): Option[JValue] = try {
    Some(parse(s))
  } catch {
    case _: Throwable => None
  }

  implicit def validationToEither[E, R](validation: Validation[E, R]): Either[HerokuRestClientException, R] = validation match {
    case Success(m) => Right(m)
    case Failure(e) => Left(new HerokuRestClientException(e.toString))
  }

  def config: Config = new Config {

    /** Update the app config vars.
      *
      * This has to be done in a peculiar way (due to heroku's implementation).
      * To remove a var you set it to `null` eg: {"myvar": null}
      * This means that we first need to pull down all the vars,
      * then if the key isn't in the incoming data map set it to null else set it to the map value
      * @param apiKey
      * @param app
      * @param data
      * @return
      */
    def set(apiKey: String, app: String, data: Map[String, String]): Either[HerokuRestClientException, Map[String, String]] = {

      get(apiKey, app) match {
        case Right(current) => {
          val toSend = dataMapToJsonMap(createMap(current, data)) -- ReservedKeys
          val configUrl = s"$base/$app/config-vars"
          val rb = request(apiKey, configUrl).PATCH << compact(render(JObject(toSend.toList)))
          val result: Future[String] = Http(rb > as.String)
          val resultString = Await.result(result, 4.seconds)
          for {
            json <- parseString(resultString).toSuccess("Couldn't parse json")
            map <- json.extractOpt[Map[String, String]].toSuccess("Couldn't convert json to Map[String,String]")
          } yield map
        }
        case Left(e) => Left(e)
      }
    }

    /**
     * Creates a new map that is a combination of the 2 maps, but if the key is not present in the incoming map the value is set to None
     * Map("A"->"Apple")
     * Map("C" -> "Carrot")
     * --> Map("A" -> None, "C" -> Some("Carrot") )
     */
    private def createMap(current: Map[String, String], incoming: Map[String, String]): Map[String, Option[String]] = {

      val merged = current ++ incoming

      val prepped: Map[String, Option[String]] = merged.map {
        tuple =>
          val value = if (incoming.get(tuple._1).isDefined) Some(tuple._2) else None
          (tuple._1, value)
      }
      prepped
    }

    private def dataMapToJsonMap(m:Map[String,Option[String]]) : Map[String,JValue] = m.map( tpl => (tpl._1, tpl._2.map(JString(_)).getOrElse(JNull)))

    def get(apiKey: String, app: String): Either[HerokuRestClientException, Map[String, String]] = {

      val configUrl = s"$base/$app/config-vars"

      val rb = request(apiKey, configUrl)
      val result: Future[String] = Http(rb > as.String)

      val resultString = Await.result(result, 4.seconds)

      for {
        json <- parseString(resultString).toSuccess("Couldn't parse json")
        map <- json.extractOpt[Map[String, String]].toSuccess("Couldn't convert json to Map[String,String]")
      } yield map
    }

  }

  def releases: Releases = new Releases {

    def list(apiKey: String, app: String): Either[HerokuRestClientException, List[Release]] = {

      val url = s"$base/$app/releases"
      val rb = request(apiKey,url, None)

      val result: Future[String] = Http(rb > as.String)
      val resultString = Await.result(result, 4.seconds)

      for {
        json <- parseString(resultString).toSuccess("Couldn't parse json")
        map <- json.extractOpt[List[Release]].toSuccess("Couldn't convert json to Map[String,String]")
      } yield map
    }
  }
}
