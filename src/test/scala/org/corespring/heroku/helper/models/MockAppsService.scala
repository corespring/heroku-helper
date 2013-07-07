package org.corespring.heroku.helper.models

import org.corespring.heroku.rest.models.unsupported.Release


class MockAppsService(
                       val apps: List[HerokuApp] = List(),
                       val branches: List[String] = List(),
                       val reservedEnvVars: List[String] = List(),
                       config: Option[HelperAppConfig] = None,
                       release: Release = new Release(),
                       releases: List[Release] = List(),
                       herokuConfigVars: Map[String, String] = Map()
                       )
  extends AppsService {

  var vars : Map[String,String] = null

  def loadConfigFor(app: HerokuApp): Option[HelperAppConfig] = config

  def currentRelease(app: HerokuApp): Release = release

  def releases(app: HerokuApp): List[Release] = releases

  def shortCommitHash = "XXXX"

  def loadHerokuConfigVars(app: HerokuApp): Map[String, String] = herokuConfigVars

  /** Set the env vars on the given heroku app */
  def setHerokuConfigVars(app: String, vars: Map[String, String]): Either[String, Map[String, String]] = {
    this.vars = vars
    Right(vars)}
}

