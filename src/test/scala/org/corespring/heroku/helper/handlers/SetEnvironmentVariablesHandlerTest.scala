package org.corespring.heroku.helper.handlers

import org.specs2.mutable.Specification
import org.corespring.heroku.helper.models.{HerokuApp, MockAppsService, EnvironmentVariables}
import org.corespring.heroku.helper.shell.LoggingShell

class SetEnvironmentVariablesHandlerTest extends Specification {

  "SetEnvironmentVariablesHandler" should {


    "set the vars" in {

      val mockAppService = new MockAppsService( apps = List(
        HerokuApp(name = "my-app", gitRemote = "my-app-git-remote")
      ))

      val mockVars = List(
        new EnvironmentVariables(herokuName = "my-app", vars = Map(("one" -> "1")))
      )
      val mockShell = new LoggingShell()
      val handler = new SetEnvironmentVariablesHandler(mockAppService, mockVars, mockShell)

      handler.runCommand("set-env-vars", "my-app")
      mockShell.cmds === List("heroku config:set one=1 --app my-app")
    }
  }

}
