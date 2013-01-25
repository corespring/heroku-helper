package org.corespring.heroku.rest.models

import collection.immutable.HashMap

case class Release(
                    env : Map[String,String] = HashMap[String,String](),
                    commit : String = "",
                    user : String = "",
                    created_at : String = "",
                    descr : String = "",
                    pstable: Map[String,String] = HashMap[String,String](),
                    name: String = "",
                    addons : Seq[String] = Seq())

