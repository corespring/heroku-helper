package org.corespring.heroku.helper.handlers

import grizzled.readline.LineToken
import grizzled.readline.{Cursor, Delim}
import org.corespring.heroku.helper.models._
import org.corespring.heroku.helper.shell.LoggingShell
import org.corespring.heroku.helper.shell.git.GitInfo
import org.corespring.heroku.helper.testUtils.RemoveFileAfter
import org.specs2.mutable.Specification

class PushHandlerTest extends Specification {

  "PushHandler" should {

    class MockGitInfo(val repos: List[(String, String)] = List(), val branches: List[String] = List()) extends GitInfo {
      def shortCommitHash: String = "XXXX"
    }

    val mockApps = new MockAppsService(
      apps = List(HerokuApp("git_one", "one"), HerokuApp("git_two", "two")),
      branches = List("branch_one", "branch_two")
    )

    val handler = new PushHandler(mockApps, new LoggingShell(""), List(), false)

    "complete repo correctly" in {

      val out = handler.complete("on",
        List(
          LineToken("push"),
          Delim,
          LineToken("on"),
          Cursor), "push on")
      out === List("one")
    }

    "list all options for repo if nothing set" in {
      val out = handler.complete("",
        List(LineToken("push"), Delim, Cursor), "push ")

      out === List("one", "two")
    }

    "complete branch correctly" in {

      val out = handler.complete("b",
        List(
          LineToken("push"),
          Delim,
          LineToken("one"),
          Delim,
          LineToken("b"),
          Cursor), "push one b")
      out === List("branch_one", "branch_two")
    }


    "run the command" in new RemoveFileAfter {

      def filesToDeleteAfter: List[String] = List(".heroku-helper-tmp-my-cool-heroku-app.json")

      val shellLog = new LoggingShell

      val mockConfig = new HelperAppConfig(name = "my-cool-heroku-app",
        push = new Push(
          before = Seq("before 1", "before 2"),
          after = Seq("after 1", "after 2"))
      )

      val mockApp = HerokuApp(gitRemote = "heroku", name = "my-cool-heroku-app")

      val mockApps = new MockAppsService(
        config = Some(mockConfig),
        apps = List(mockApp),
        branches = List("master"))

      val expectedTemplate = """before 1 ${tmpFile} ${appName} master
                               |before 2 ${tmpFile} ${appName} master
                               |git push heroku master:master
                               |after 1 ${tmpFile} ${appName} master
                               |after 2 ${tmpFile} ${appName} master
                               | """.stripMargin

      import org.corespring.heroku.helper.string.utils._

      val handler = new PushHandler(mockApps, shellLog, List(), false)
      val expected = interpolate(expectedTemplate, ("tmpFile", handler.configFilename(mockApp)), ("appName", mockApp.name))
      handler.runCommand("push", "my-cool-heroku-app master")
      shellLog.cmds.mkString("\n") === expected.trim
    }
  }
}

