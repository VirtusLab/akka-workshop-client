name := "akka-workshop-client"

version := "2.0"

scalaVersion := "2.12.5"

val akkaVersion     = "2.5.14"
val akkaHttpVersion = "10.1.3"

libraryDependencies ++= Seq(
  "commons-codec"     % "commons-codec"         % "1.11",
  "com.typesafe.akka" %% "akka-actor"           % akkaVersion,
  "com.typesafe.akka" %% "akka-remote"          % akkaVersion,
  "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion
)
