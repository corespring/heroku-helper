package org.corespring.heroku.helper.handlers

import org.specs2.mutable.Specification
import grizzled.readline.{CompleterHelper, CompletionToken, LineToken, Cursor, Delim}
import grizzled.cmd.{KeepGoing, CommandAction}

class BaseHandlerTest extends Specification with CompleterHelper {

  def tokenize(s: String) = {
    val split = s.split(" ").toList
    val withDelims = split match {
      case List(oneItem) => mapWithDelims(List(oneItem))
      case _ => mapWithDelims(split)
    }

    withDelims.map {
      token => token match {
        case LineToken("^C") => Cursor
        case other: CompletionToken => other
      }
    }
  }

  class ContextualMockHandler(options: (String => List[String])*) extends BaseHandler {

    val CommandName = "mock"
    val Help = "just a mock handler"

    override def complete(token: String,
                          allTokens: List[CompletionToken],
                          line: String): List[String] = {

      completeContextually(token, allTokens, line, options: _*)
    }

    override def runCommand(command: String, args: String): CommandAction = KeepGoing
  }

  class MockHandler(options: List[String]*) extends BaseHandler {

    val CommandName = "mock"
    val Help = "just a mock handler"

    override def complete(token: String,
                          allTokens: List[CompletionToken],
                          line: String): List[String] = {

      completeFromOptions(token, allTokens, line, options: _*)
    }

    override def runCommand(command: String, args: String): CommandAction = KeepGoing
  }

  val handler = new MockHandler(List("one", "two"), List("a", "b"), List("car", "bike"))

  val contextualMockHandler = new ContextualMockHandler(
    ((s) => List("one", "two")),
    ((s) => if (s == "one") List("a", "b") else List("c", "d"))
  )

  "BaseHandler" should {

    def assertCompleteInput(handler:BaseHandler, cmd:String,expected:List[String]) = {
       val tokens = tokenize(cmd) :+ Cursor
       handler.complete("", tokens, cmd) === expected 
    }

    "complete using letters in a sequence" in {
       val handler = new MockHandler(List("car","far","maritime","mart","hair"))
       assertCompleteInput(handler, "mock ar", List("car", "far", "maritime", "mart", "hair"))
       assertCompleteInput(handler, "mock mat", List("maritime", "mart"))
       assertCompleteInput(handler, "mock mai", List("maritime"))
    }

    "handle one level of options" in {
      val cmdString = "mock "
      val tokens = tokenize(cmdString) ::: List(Delim, Cursor)
      println(tokens)
      handler.complete("", tokens, cmdString) === List("one", "two")
    }

    "handle one level of options - with string" in {
      val cmdString = "mock on"
      val tokens = tokenize(cmdString) :+ Cursor
      println(tokens)
      handler.complete("", tokens, cmdString) === List("one")
    }

    "handle two level of options" in {
      val cmdString = "mock one "
      val tokens = tokenize(cmdString) ::: List(Delim, Cursor)
      handler.complete("", tokens, cmdString) === List("a", "b")
    }

    "handle two level of options - with string" in {
      val cmdString = "mock one b"
      val tokens = tokenize(cmdString) :+ Cursor
      handler.complete("", tokens, cmdString) === List("b")
    }

    "handle three level of options" in {
      val cmdString = "mock one a "
      val tokens = tokenize(cmdString) ::: List(Delim, Cursor)
      handler.complete("", tokens, cmdString) === List("car", "bike")
    }


    "handle three level of options - with string" in {
      val cmdString = "mock one a c"
      val tokens = tokenize(cmdString) :+ Cursor
      handler.complete("", tokens, cmdString) === List("car")
    }

    "return an empty list if options are exhausted" in {
      val cmdString = "mock one a car "
      val tokens = tokenize(cmdString) ::: List(Delim, Cursor)
      handler.complete("", tokens, cmdString) === List()
    }

    "handle contextual options" in {
      val cmdString = "mock "
      val tokens = tokenize(cmdString) ::: List(Delim, Cursor)
      println(tokens)
      contextualMockHandler.complete("", tokens, cmdString) === List("one", "two")
    }

    "handle contextual options - with string" in {
      val cmdString = "mock on"
      val tokens = tokenize(cmdString) :+ Cursor
      println(tokens)
      contextualMockHandler.complete("", tokens, cmdString) === List("one")
    }


    "handle two levels of contextual options" in {
      val cmdString = "mock two "
      val tokens = tokenize(cmdString) ::: List(Delim, Cursor)
      println(tokens)
      contextualMockHandler.complete("", tokens, cmdString) === List("c", "d")
    }
  }

  "handle two levels of contextual options - with string" in {
    val cmdString = "mock two c"
    val tokens = tokenize(cmdString) :+ Cursor
    println(tokens)
    contextualMockHandler.complete("", tokens, cmdString) === List("c")
  }

}
