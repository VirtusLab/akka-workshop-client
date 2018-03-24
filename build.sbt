name := """akka-workshop-client"""

version := "1.0"

scalaVersion := "2.12.5"

val akkaVersion = "2.5.11"

libraryDependencies ++= Seq(
  "commons-codec" % "commons-codec" % "1.11",
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "junit" % "junit" % "4.11" % "test",
  "com.novocode" % "junit-interface" % "0.10" % "test"
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")
