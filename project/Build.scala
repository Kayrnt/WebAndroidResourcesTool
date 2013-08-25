import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName = "AkkaAndroidStringTool"
  val appVersion = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.1.4",
    "com.typesafe.akka" %% "akka-slf4j" % "2.1.4",
    "org.webjars" %% "webjars-play" % "2.1.0-2",
    "org.webjars" % "bootstrap" % "2.3.1",
    "org.webjars" % "flot" % "0.8.0",
    "com.typesafe.akka" %% "akka-testkit" % "2.1.4" % "test",
    "net.lingala.zip4j" % "zip4j" % "1.3.1"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings()

}
