name := "akka-workshop-client"

version := "1.0"

scalaVersion := "2.11.1"

resolvers += "Workshop Repository" at "http://headquarters:8081/artifactory/libs-release-local"

libraryDependencies ++= Seq(
  "commons-codec" % "commons-codec" % "1.9",
  "com.typesafe.akka" %% "akka-actor" % "2.4.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.3",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "com.virtuslab" %% "akka-workshop-distributor" % "1.0.1",
  "com.virtuslab" %% "akka-workshop-decrypter" % "1.0.1"
)
