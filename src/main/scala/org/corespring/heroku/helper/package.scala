package org.corespring.heroku

package object helper {

  object exceptions {
    class HerokuHelperException(val message: String) extends Exception(message)
  }
}
