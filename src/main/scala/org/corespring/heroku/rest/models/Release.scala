package org.corespring.heroku.rest.models

import collection.immutable.HashMap

object unsupported {

  /** A model of a heroku release
    * Note: This is using the current implementation of the heroku api that is no longer supported:
    * https://api-docs.heroku.com/releases
    * At some point we need to move the the new api:
    * https://devcenter.heroku.com/articles/platform-api-reference#release
    *
    * However - this api is in beta and it doesn't provide any information on how to get the env for a given release.
    *
    *
    * @param env
    * @param commit
    * @param user
    * @param created_at
    * @param descr
    * @param pstable
    * @param name
    * @param addons
    */
  case class Release(
                      env: Map[String, String] = HashMap[String, String](),
                      commit: String = "",
                      user: String = "",
                      created_at: String = "",
                      descr: String = "",
                      pstable: Map[String, String] = HashMap[String, String](),
                      name: String = "",
                      addons: Seq[String] = Seq())

}

object beta {

  case class ReleaseUser(email: String, id: String)

  /** A representation of the new Beta Release model
    * https://devcenter.heroku.com/articles/platform-api-reference#release
    */
  case class Release(created_at: String,
                     description: String,
                     id: String,
                     version: Int,
                     updated_at: String,
                     user: ReleaseUser) {

    val DeployRegex = """Deploy (.{7})$""".r

    def commit: Option[String] = {
      if (description.matches(DeployRegex.pattern.pattern)) {
        val DeployRegex(out) = description
        Some(out)
      } else {
        None
      }
    }
  }

}
