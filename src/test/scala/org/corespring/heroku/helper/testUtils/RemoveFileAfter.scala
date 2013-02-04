package org.corespring.heroku.helper.testUtils

import org.specs2.mutable.After
import java.io.File

trait RemoveFileAfter extends After {

  def filesToDeleteAfter: List[String]

  def after = {
    filesToDeleteAfter.foreach {
      p =>
        try {
          new File(p).delete
        }
        catch {
          case e: Throwable => //do nothing
        }
    }
  }
}
