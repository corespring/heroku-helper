package org.corespring.heroku.helper.shell

import git.GitInfo

class Git(shell:Shell) extends GitInfo{

  val RemoteLineRegex = """(.*)[\s|\t]*?git@heroku.com:(.*)\.git \(fetch\)""".r

  def repos: List[(String, String)] = {

    def parseGitRemote(s:String) : List[(String,String)] = {
      val maybeRepos = s.split("\n").toList.map{ line =>
        try {
          val RemoteLineRegex(remoteName, herokuName) = line
          Some((remoteName.trim, herokuName.trim))
        } catch {
          case e : MatchError => None
        }
      }
      maybeRepos.flatten
    }

    val remotes = shell.run("git remote -v")
    parseGitRemote(remotes)
  }

  def branches : List[String] = {
    val branches = shell.run("git branch")

    def parseBranches(s:String) : List[String] = {
      val LineRegex = """[\*|\s|\t]*(.*)""".r

      s.split("\n").toList.map { line =>
        val LineRegex(name) = line
        name
      }
    }
    parseBranches(branches)
  }

}

object Git extends Git(Shell)
