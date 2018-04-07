name := "akka-workshop-client"

version := "2.0"

scalaVersion := "2.12.5"

resolvers += "Workshop Repository" at "http://headquarters:8081/artifactory/libs-release-local"

val akkaVersion = "2.5.11"

libraryDependencies ++= Seq(
  "commons-codec" % "commons-codec" % "1.11",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion
)
