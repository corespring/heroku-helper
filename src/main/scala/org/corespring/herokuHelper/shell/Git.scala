package org.corespring.herokuHelper.shell

object Git {

  val RemoteLineRegex = """(.*)[\s|\t]*?git@heroku.com:(.*)\.git \(fetch\)""".r

  def repos: List[(String, String)] = {
    val remotes = Shell.run("git remote -v")
    parseGitRemote(remotes)
  }

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

}
