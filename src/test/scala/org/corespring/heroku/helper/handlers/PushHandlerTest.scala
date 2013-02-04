package org.corespring.heroku.helper.handlers

import org.specs2.mutable.{After, Specification}
import grizzled.readline.{Cursor, Delim, LineToken, CompletionToken}
import org.corespring.heroku.helper.shell.git.GitInfo
import org.corespring.heroku.helper.shell.{MockShell, CmdResult, Shell}
import org.corespring.heroku.helper.models._
import org.corespring.heroku.helper.models.HerokuAppConfig
import org.corespring.heroku.helper.models.Config
import org.corespring.heroku.helper.models.Push
import grizzled.readline.LineToken
import org.corespring.heroku.rest.models.Release
import org.corespring.heroku.helper.testUtils.RemoveFileAfter

class PushHandlerTest extends Specification {

  "PushHandler" should {

    class MockGitInfo(repos: List[(String, String)] = List(), branches: List[String] = List()) extends GitInfo {
      def repos(): List[(String, String)] = repos

      def branches(): List[String] = branches

      def shortCommitHash: String = "XXXX"
    }

    class MockConfigLoader(config: Config = new Config) extends ConfigLoader {
      def load(): Config = config

      def save(config: Config) {}
    }

    val mockApps = new MockAppsService(
      apps = List(HerokuApp("git_one", "one"), HerokuApp("git_two", "two")),
      branches = List("branch_one", "branch_two")
    )

    val handler = new PushHandler(mockApps, new MockShell(""))

    "complete repo correctly" in {

      val out = handler.complete("o",
        List(
          LineToken("push"),
          Delim,
          LineToken("o"),
          Cursor), "push o")
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

      val shellLog = new MockShell

      val mockConfig = new HerokuAppConfig(name = "my-cool-heroku-app",
        push = new Push(
          before = Seq("before 1", "before 2"),
          after = Seq("after 1", "after 2"))
      )

      val mockApp = HerokuApp(gitRemote = "heroku", name = "my-cool-heroku-app")

      val mockApps = new MockAppsService(
        config = Some(mockConfig),
        apps = List(mockApp),
        branches = List("master"))

      val handler = new PushHandler(mockApps, shellLog)

      val expectedTemplate = """before 1 ${tmpFile} ${appName}
                               |before 2 ${tmpFile} ${appName}
                               |git push heroku master:master
                               |after 1 ${tmpFile} ${appName}
                               |after 2 ${tmpFile} ${appName}
                               | """.stripMargin

      import org.corespring.heroku.helper.string.utils._

      val expected = interpolate(expectedTemplate,
        ("tmpFile", handler.configFilename(mockApp)), ("appName", mockApp.name))
      handler.runCommand("push", "my-cool-heroku-app master")
      shellLog.cmds.mkString("\n") === expected.trim
    }
  }

}

