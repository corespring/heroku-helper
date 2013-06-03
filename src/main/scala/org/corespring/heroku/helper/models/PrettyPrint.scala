package org.corespring.heroku.helper.models

import org.corespring.heroku.rest.models.Release

object PrettyPrint {
  def release(r: Release): String = {

    val template =
      """|-------------------------------------
        |Release Name: ${name} / Commit: ${commit} / Created: ${created}
        |User: ${user}
        |Env Vars: ${envVars}""".stripMargin


    org.corespring.heroku.helper.string.utils.interpolate(template,
      ("name", r.name),
      ("commit", r.commit, "none given"),
      ("created", r.created_at),
      ("user", r.user),
      ("envVars", r.env.map(kv => kv._1 + ": " + kv._2).mkString("\n")))
  }


  def config(c: HelperAppConfig): String = {
    val template = """
                     |push:
                     |  cmd: ${pushCmd}
                     |  before:
                     |  ${beforePush}
                     |  after:
                     |  ${afterPush}
                     |rollback:
                     |  cmd: ${rollbackCmd}
                     |  before:
                     |  ${beforeRollback}
                     |  after:
                     |  ${afterRollback}
                     |
                   """.stripMargin

    org.corespring.heroku.helper.string.utils.interpolate(template,
      ("pushCmd", tidy(c.push.cmd)),
      ("rollbackCmd", tidy(c.rollback.cmd)),
      ("beforePush", c.push.before.mkString("\n")),
      ("afterPush", c.push.after.mkString("\n")),
      ("beforeRollback", c.rollback.before.mkString("\n")),
      ("afterRollback", c.rollback.after.mkString("\n"))
    )

  }

  private def tidy(s: String): String = s
    .replaceAll("\\$", "\\\\\\$")
    .replaceAll("\\{", "\\\\\\{")
    .replaceAll("\\}", "\\\\\\}")
}
