package org.corespring.heroku.helper.handlers

import org.specs2.mutable.Specification
import grizzled.readline.{Cursor, Delim, LineToken, CompletionToken}
import org.corespring.heroku.helper.shell.git.GitInfo
import org.corespring.heroku.helper.shell.Shell
import org.corespring.heroku.helper.models._
import org.corespring.heroku.helper.models.HerokuAppConfig
import org.corespring.heroku.helper.models.Config
import org.corespring.heroku.helper.models.Push
import grizzled.readline.LineToken
import org.corespring.heroku.rest.models.Release

class PushHandlerTest extends Specification {

  "PushHandler" should {

    class MockGitInfo(repos: List[(String, String)] = List(), branches: List[String] = List()) extends GitInfo {
      def repos(): List[(String, String)] = repos

      def branches(): List[String] = branches
    }

    class MockConfigLoader(config: Config = new Config) extends ConfigLoader {
      def config(): Config = config

      def saveConfig(config: Config) {}
    }

    class MockAppsService(apps: List[HerokuApp] = List(),
                          config: Option[HerokuAppConfig] = None,
                          release: Release = new Release(),
                          branches: List[String] = List(),
                          releases: List[Release] = List()
                           ) extends AppsService {
      def apps(): List[HerokuApp] = apps

      def loadConfigFor(app: HerokuApp): Option[HerokuAppConfig] = config

      def currentRelease(app: HerokuApp): Release = release

      def branches(): List[String] = branches

      def releases(app: HerokuApp): List[Release] = releases
    }

    val mockApps = new MockAppsService(
      apps = List(HerokuApp("one", "one"), HerokuApp("two", "two")),
      branches = List("branch_one", "branch_two")
    )

    val handler = new PushHandler(
      mockApps,
      new Shell {
        /** Execute a command and return the response */
        def run(cmd: String): String = cmd
      })

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


    "run the command" in {

      val shellLog = new Shell {
        var log: String = ""

        def run(cmd: String): String = {
          log += (cmd + "\n")
          cmd
        }
      }

      val mockConfig = new HerokuAppConfig(name = "my-cool-heroku-app",
        gitRemoteName = "heroku",
        push = new Push(
          before = Seq("before 1", "before 2"),
          after = Seq("after 1", "after 2"))
      )


      val mockApps = new MockAppsService(
        config = Some(mockConfig),
        apps = List(HerokuApp(gitRemote = "heroku", name = "my-cool-heroku-app")),
        branches = List("master"))

      val handler = new PushHandler(mockApps, shellLog)

      val expected = """before 1
                       |before 2
                       |git push heroku master:master
                       |after 1
                       |after 2
                       | """.stripMargin

      handler.runCommand("push", "heroku master")
      shellLog.log.trim === expected.trim
    }
  }


}
