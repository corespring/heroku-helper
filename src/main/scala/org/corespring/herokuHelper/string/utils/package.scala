package org.corespring.herokuHelper.string

package object utils {

  import util.matching.Regex

  val DefaultRegex = """\$\{([^}]+)\}""".r

  def interpolate(text: String, lookup: String => String, regex: Regex = DefaultRegex) =
    regex.replaceAllIn(text, (_: scala.util.matching.Regex.Match) match {
      case Regex.Groups(v) => {
        val result = lookup(v)
        result
      }
    })

}
