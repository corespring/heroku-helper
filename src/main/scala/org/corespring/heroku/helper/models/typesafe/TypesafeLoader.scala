package org.corespring.heroku.helper.models.typesafe

import com.typesafe.config.{ConfigFactory, Config}

trait TypesafeLoader {

  protected def loadTypesafeConfig(path:String) : Config = {
    val configString = scala.io.Source.fromFile(path).mkString
    ConfigFactory.parseString(configString)
  }


  /** Load a property from the Typesafe config object.
    * If an error is thrown provide a default
    * @param dataFn - the function that returns the data and may throw an exception
    * @param convertor - the function that converts it from A => B
    * @param default - the default value
    * @tparam A - A Typesafe config type
    * @tparam B - The return type
    * @return
    */
  protected def loadWithDefault[A, B](dataFn: (() => A), convertor: (A => B), default: B): B = {
    try {
      convertor(dataFn())
    }
    catch {
      case e: Throwable =>  default
    }
  }

  protected def toSome[A](thing:A):Option[A] = Some(thing)

  protected def toScalaList[A](javaList:java.util.List[A]) : List[A] = {
    import scala.collection.JavaConversions._
    javaList.toList
  }


}
