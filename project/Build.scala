import sbt._
import Keys._


object Build extends sbt.Build {

  val name = "heroku-helper"
  val appVersion = "0.2-SNAPSHOT"

  def buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "org.corespring",
    scalaVersion := "2.9.2",
    version := appVersion,
    libraryDependencies ++= Dependencies.all,
    resolvers ++= Resolvers.all
  )

  object Dependencies {

    val all = Seq(
      "org.clapper" %% "grizzled-scala" % "1.0.13",
      "com.codahale" % "jerkson_2.9.1" % "0.5.0",
      "org.specs2" %% "specs2" % "1.12.2" % "test",
      "com.typesafe" % "config" % "1.0.0",
      "org.scalaj" %% "scalaj-http" % "0.3.6"
    )
  }

  object Resolvers {

    val all = Seq(
      "Sbt plugins" at "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/",
      "Sonatype OSS" at "http://oss.sonatype.org/content/repositories/releases/",
      "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
      "mvn repo" at "http://repo1.maven.org/maven2/",
      "repo.novus rels" at "http://repo.novus.com/releases/",
      "repo.novus snaps" at "http://repo.novus.com/snapshots/",
      "codahale repo" at "http://repo.codahale.com")
  }


  lazy val root = Project(id = name, base = file("."), settings = buildSettings)



}