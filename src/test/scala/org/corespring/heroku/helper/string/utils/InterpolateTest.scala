package org.corespring.heroku.helper.string.utils

import org.specs2.mutable.Specification

class InterpolateTest extends Specification {

  "interpolate" should {
    "work" in {
      val template = """Name: ${name}
                       |Commit: ${commit}
                       |Created: ${created}
                       |User: ${user}
                       |Env Vars: ${envVars}""".stripMargin

      val values : Array[Product] = Array(
        ("name", "v1"),
        ("commit", null, "?"),
        ("created", "2013/01/23 06:20:06 -0800"),
        ("user", "ed.eustace@gmail.com"),
        ("envVars", "")
      )

      interpolate(template, values: _*).trim ===
        """
          |Name: v1
          |Commit: ?
          |Created: 2013/01/23 06:20:06 -0800
          |User: ed.eustace@gmail.com
          |Env Vars:
        """.stripMargin.trim
    }
  }

}
