package org.corespring.heroku.helper.handlers

import org.specs2.mutable.Specification
import org.corespring.heroku.helper.models.{Rollback, HelperAppConfig, MockAppsService, HerokuApp}
import org.corespring.heroku.rest.models.{Release}
import org.corespring.heroku.helper.shell.LoggingShell
import org.corespring.heroku.helper.testUtils.RemoveFileAfter

class RollbackHandlerTest extends Specification {

  "RollbackHandler" should {
    "rollback" in new RemoveFileAfter {

      def filesToDeleteAfter: List[String] = List(".rollback-v1")

      val mockService = new MockAppsService(
        apps = List(
          HerokuApp(name = "my-app", gitRemote = "my-app-git-remote")
        ),
        config = Some(
          HelperAppConfig(
            name = "my-app",
            rollback = new Rollback(
              before = Seq("before.1.sh", "before.2.sh"),
              after = Seq("after.1.sh", "after.2.sh")
            )
          )
        ),
        releases = List(
          Release(name = "v1", commit = "commit_hash")
        )
      )
      val mockShell = new LoggingShell()


      val handler = new RollbackHandler(mockService, mockShell)

      handler.runCommand(handler.CommandName, "my-app v1")

      val expected =
        """before.1.sh commit_hash .rollback-v1
          |before.2.sh commit_hash .rollback-v1
          |heroku releases:rollback v1 --app my-app
          |after.1.sh commit_hash .rollback-v1
          |after.2.sh commit_hash .rollback-v1""".stripMargin

      mockShell.cmds.mkString("\n") === expected
    }
  }

}
