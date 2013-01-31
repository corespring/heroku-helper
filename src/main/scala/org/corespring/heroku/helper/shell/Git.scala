package org.corespring.heroku.helper.shell

import git.GitInfo

class Git(shell: Shell) extends GitInfo {

  val RemoteLineRegex = """(.*)[\s|\t]*?git@heroku.com:(.*)\.git \(fetch\)""".r


  private def run(cmd: String): String = {

    val result = shell.run(cmd)

    if (result.exitCode == 0) {
      result.out
    }
    else {
      throw new RuntimeException("Error running: " + cmd + " result err: " + result.err)
    }
  }

  def repos: List[(String, String)] = {

    def parseGitRemote(s: String): List[(String, String)] = {
      val maybeRepos = s.split("\n").toList.map {
        line =>
          try {
            val RemoteLineRegex(remoteName, herokuName) = line
            Some((remoteName.trim, herokuName.trim))
          } catch {
            case e: MatchError => None
          }
      }
      maybeRepos.flatten
    }

    val output = run("git remote -v")

    parseGitRemote(output)
  }

  def branches: List[String] = {
    val branches = run("git branch")

    def parseBranches(s: String): List[String] = {
      val LineRegex = """[\*|\s|\t]*(.*)""".r

      s.split("\n").toList.map {
        line =>
          val LineRegex(name) = line
          name
      }
    }
    parseBranches(branches)
  }

  def shortCommitHash: String = run("git rev-parse --short HEAD").trim
}

object Git extends Git(Shell)
