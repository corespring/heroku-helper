package org.corespring.heroku.rest.client

import org.corespring.heroku.rest.exceptions.HerokuRestClientException
import org.corespring.heroku.rest.models.unsupported.Release

trait HerokuRestClient{

  def config : Config
  def releases : Releases

  trait Config{
    def get(apiKey:String, app:String) : Either[HerokuRestClientException,Map[String,String]]
    def set(apiKey:String, app:String, data : Map[String,String]) : Either[HerokuRestClientException,Map[String,String]]
  }

  trait Releases{
    /** Get the release list - Note: This is currently using the unsupported old heroku api.
     * The new api is in beta - will move to that once in place but at the moment its release model doesn't contain env vars.
     * @return
     */
    def list(apiKey: String, app: String): Either[HerokuRestClientException, List[Release]]
  }
}

object NullClient extends HerokuRestClient{
  def config: Config = new Config {
    def set(apiKey: String, app: String, data: Map[String, String]): Either[HerokuRestClientException, Map[String, String]] = ???

    def get(apiKey: String, app: String): Either[HerokuRestClientException, Map[String, String]] = ???
  }

  def releases: Releases = new Releases {
    def list(apiKey: String, app: String): Either[HerokuRestClientException, List[Release]] = ???
  }
}

