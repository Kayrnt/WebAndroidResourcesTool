import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName = "AkkaAndroidStringTool"
  val appVersion = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.2.1",
    "com.typesafe.akka" %% "akka-slf4j" % "2.2.1",
    "com.typesafe.akka" %% "akka-testkit" % "2.2.1" % "test",
      "commons-io" % "commons-io" % "2.4"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings()

}
