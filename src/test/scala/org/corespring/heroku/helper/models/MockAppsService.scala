package org.corespring.heroku.helper.models

import org.corespring.heroku.rest.models.Release


class MockAppsService(apps: List[HerokuApp] = List(),
                      config: Option[HerokuAppConfig] = None,
                      release: Release = new Release(),
                      branches: List[String] = List(),
                      releases: List[Release] = List()
                       ) extends AppsService {
  def apps(): List[HerokuApp] = apps

  def loadConfigFor(app: HerokuApp): Option[HerokuAppConfig] = config

  def currentRelease(app: HerokuApp): Release = release

  def branches(): List[String] = branches

  def releases(app: HerokuApp): List[Release] = releases

  def shortCommitHash = "XXXX"

  def loadHerokuConfigFor(app: HerokuApp): Map[String, String] = Map()
}

