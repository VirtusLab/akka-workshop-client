name := "akka-workshop-client"

version := "2.0"

scalaVersion := "2.12.5"

resolvers += "Workshop Repository" at "http://headquarters:8081/artifactory/libs-release-local"

libraryDependencies ++= Seq(
  "commons-codec" % "commons-codec" % "1.11",
  "com.typesafe.akka" %% "akka-actor" % "2.5.11"
)
