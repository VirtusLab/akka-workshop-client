val http4sVersion = "0.19.0-M1"
val LogbackVersion = "1.2.3"

lazy val root = (project in file("."))
  .settings(
    organization := "com.virtuslab",
    name := "akka-workshop-client",
    version := "2.0",
    scalaVersion := "2.12.6",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "io.circe" %% "circe-generic" % "0.10.0-M1",
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "commons-codec" % "commons-codec" % "1.11"
    )
  )