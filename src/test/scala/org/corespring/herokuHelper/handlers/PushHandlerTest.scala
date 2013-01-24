package org.corespring.herokuHelper.handlers

import org.specs2.mutable.Specification
import grizzled.readline.{Cursor, Delim, LineToken, CompletionToken}
import org.corespring.herokuHelper.shell.git.GitInfo

class PushHandlerTest extends Specification {

  "PushHandler" should {
    val handler = new PushHandler(new GitInfo {
      def repos: List[(String, String)] = List(("one", "one"), ("two", "two"))
      def branches : List[String] = List("branch_one", "branch_two")
    })

    "complete repo correctly" in {

      val out = handler.complete("o",
        List(
          LineToken("push"),
          Delim,
          LineToken("o"),
          Cursor ), "push o")
      out === List("one")
    }

    "list all options for repo if nothing set" in {
      val out = handler.complete("",
      List(LineToken("push"), Delim, Cursor), "push ")

      out === List("one","two")
    }

    "complete branch correctly" in {

      val out = handler.complete("b",
        List(
          LineToken("push"),
          Delim,
          LineToken("one"),
          Delim,
          LineToken("b"),
          Cursor ), "push one b")
      out === List("branch_one", "branch_two")
    }
  }


}
