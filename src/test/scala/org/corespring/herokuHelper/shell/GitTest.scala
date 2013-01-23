package org.corespring.herokuHelper.shell

import org.specs2.mutable.Specification

class GitTest extends Specification {

  "Git" should {
    "parse remote" in {

      val sample = """heroku git@heroku.com:heroku-helper-example-app.git (fetch)
                     |heroku git@heroku.com:heroku-helper-example-app.git (push)
                     |origin git@github.com:corespring/heroku-helper-example-app.git (fetch)
                     |origin git@github.com:corespring/heroku-helper-example-app.git (push)""".stripMargin
      Git.parseGitRemote(sample) === List(("heroku", "heroku-helper-example-app"))

      val sampleTwo = """heroku git@heroku.com:heroku-helper-example-app.git (fetch)
                        |other-app git@heroku.com:my-other-app.git (fetch)
                        |heroku git@heroku.com:heroku-helper-example-app.git (push)
                        |origin git@github.com:corespring/heroku-helper-example-app.git (fetch)
                        |origin git@github.com:corespring/heroku-helper-example-app.git (push)""".stripMargin

      Git.parseGitRemote(sampleTwo) === List(
        ("heroku", "heroku-helper-example-app"),
        ("other-app", "my-other-app")
      )
    }
  }

}
