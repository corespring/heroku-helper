package org.corespring.heroku.helper.models

import org.specs2.mutable.Specification


class netrcTest extends Specification {

  "netrc" should {
    val testPath = "src/test/resources/org/corespring/heroku/helper/models/netrc_files"

    "return the api key" in {

      val sample: String = """machine api.heroku.com
                             |  login ed.eustace@gmail.com
                             |  password pword
                             |machine code.heroku.com
                             |  login ed.eustace@gmail.com
                             |  password pword""".stripMargin

      netrc.apiKey(testPath + "/sample") === Some("pword")
      netrc.apiKey("") === None
      netrc.apiKey(testPath + "/sampleWithMissingLines") === None
      netrc.apiKey(testPath + "/sampleWithBadFormat") === None

    }
  }
}
