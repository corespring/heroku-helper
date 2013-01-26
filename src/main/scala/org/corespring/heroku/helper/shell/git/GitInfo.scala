package org.corespring.heroku.helper.shell.git

trait GitInfo {

  /** Get a list of local remote repositories that are heroku repositories */
  def repos  : List[(String,String)]

  /** List the local branches */
  def branches : List[String]

  /** The local git repos short commit hash*/
  def shortCommitHash : String
}
