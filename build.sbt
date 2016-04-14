name := "akka-workshop-client"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += "Workshop Repository" at "http://headquarters:8081/artifactory/libs-release-local"

libraryDependencies ++= Seq(
  "commons-codec" % "commons-codec" % "1.9",
  "com.typesafe.akka" %% "akka-actor" % "2.4.3"
)
