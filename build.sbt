import Dependencies._

ThisBuild / scalaVersion := "2.13.3"
ThisBuild / version := "1.0.0-SNAPSHOT"
ThisBuild / organization := "com.gomezgimenez"

val platform = "windows-x86_64"

lazy val `sla-printer-timelapse-tool` = (project in file("."))
  .settings(
    resolvers += "Clojars" at "https://clojars.org/repo/",
    mainClass in (Compile, run) := Some("Main"),
    mainClass in assembly := Some("Main"),
    libraryDependencies ++= List(
      json4s,
      scalaTest % Test,
      webcamCapture,
      scrImage
    )
  )
