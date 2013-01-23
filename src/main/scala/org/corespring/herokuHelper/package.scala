package org.corespring

package object herokuHelper {

  object exceptions {
    class HerokuHelperException(val message: String) extends Exception(message)
  }
}
