package org.corespring.heroku.helper.models

import org.corespring.heroku.rest.models.Release
import org.corespring.heroku.helper.shell.git.GitInfo
import org.corespring.heroku.rest.client.HerokuRestClient
import collection.mutable.HashMap


case class HerokuApp(gitRemote: String, name: String)

trait AppsService {
  def apps: List[HerokuApp]

  def releases(app: HerokuApp): List[Release]

  def currentRelease(app: HerokuApp): Release

  /** load the config for the given app
   * @param app
   * @return
   */
  def loadConfigFor(app: HerokuApp): Option[HelperAppConfig]

  /** @return - a list of branches for this git repo
    */
  def branches: List[String]

  def loadHerokuConfigVars(app:HerokuApp) : Map[String,String]

  /** The local git repos short commit hash*/
  def shortCommitHash : String

  def reservedEnvVars : List[String]

}

class AppsServiceImpl(apiKey: String, git: GitInfo, config: HelperConfig) extends AppsService {

  private val appToReleaseMap: HashMap[HerokuApp, List[Release]] = new HashMap[HerokuApp, List[Release]]()

  def apps: List[HerokuApp] = git.repos.map(tuple => HerokuApp(tuple._1, tuple._2))

  def releases(app: HerokuApp): List[Release] = {

    def loadReleases = {
      HerokuRestClient.Releases.list(apiKey, app.name) match {
        case Left(error) => null
        case Right(releases) => releases
      }
    }

    appToReleaseMap.get(app) match {
      case Some(releases) => releases
      case _ => {
        val loadedReleases = loadReleases
        appToReleaseMap.put(app, loadedReleases)
        loadedReleases
      }
    }
  }

  def currentRelease(app: HerokuApp): Release = {

    object ::> {
      def unapply[A](l: List[A]) = Some((l.init, l.last))
    }

    releases(app) match {
      case List() => throw new RuntimeException("can't load releases for app")
      case _ ::> last => last
    }

  }

  def loadConfigFor(app: HerokuApp): Option[HelperAppConfig] = config.appConfigByName(app.name)

  def reservedEnvVars = config.reservedEnvVars

  def branches : List[String] = git.branches

  def loadHerokuConfigVars(app:HerokuApp) : Map[String,String] = {
    HerokuRestClient.Config.config(apiKey,app.name) match {
      case Right(map) => map
      case Left(error) => Map()
    }
  }

  def shortCommitHash : String = git.shortCommitHash
}
