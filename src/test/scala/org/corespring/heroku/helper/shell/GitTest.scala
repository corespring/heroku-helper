package org.corespring.heroku.helper.shell

import git.GitInfo
import org.specs2.mutable.Specification

class GitTest extends Specification {

  "Git" should {


    "parse remote" in {

      val oneRemote = """heroku git@heroku.com:heroku-helper-example-app.git (fetch)
                        |heroku git@heroku.com:heroku-helper-example-app.git (push)
                        |origin git@github.com:corespring/heroku-helper-example-app.git (fetch)
                        |origin git@github.com:corespring/heroku-helper-example-app.git (push)""".stripMargin

      new Git(new MockShell(oneRemote)).repos === List(("heroku", "heroku-helper-example-app"))

      val twoRemotes = """heroku git@heroku.com:heroku-helper-example-app.git (fetch)
                         |other-app git@heroku.com:my-other-app.git (fetch)
                         |heroku git@heroku.com:heroku-helper-example-app.git (push)
                         |origin git@github.com:corespring/heroku-helper-example-app.git (fetch)
                         |origin git@github.com:corespring/heroku-helper-example-app.git (push)""".stripMargin

      new Git(new MockShell(twoRemotes)).repos === List(
        ("heroku", "heroku-helper-example-app"),
        ("other-app", "my-other-app")
      )
    }

    "parse branch" in {

      val master = """* master
                     |  new_b""".stripMargin

      new Git(new MockShell(master)).branches === List("master", "new_b")

    }
  }

}
