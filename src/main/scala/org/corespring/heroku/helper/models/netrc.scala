package org.corespring.heroku.helper.models

import grizzled.file.util.joinPath
import java.io.File

object netrc {

  val DefaultPath = joinPath(System.getProperty("user.home"), ".netrc")

  def apiKey( filePath:String = DefaultPath) : Option[String] = {

    def keyFromContents(rc: String): Option[String] = {
      val split = rc.split("\n")
      val declarationIndex = split.indexWhere(_.startsWith("machine code.heroku.com"))

      if (declarationIndex == -1 || split.length < (declarationIndex + 3))
        None
      else {
        val password = split(declarationIndex + 2)
        val PasswordRegex = """.*password\s(.*)""".r
        try {
          val PasswordRegex(key) = password
          Some(key)
        } catch {
          case e: MatchError => None
        }
      }
    }

    if(exists(filePath)) {
      keyFromContents(contents(filePath))
    } else {
      None
    }
  }

  private def exists(filePath:String = DefaultPath): Boolean = new File(filePath).exists

  private def contents(path: String): String = scala.io.Source.fromFile(path).mkString
}
