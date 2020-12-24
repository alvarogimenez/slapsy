import Dependencies._

ThisBuild / scalaVersion := "2.13.3"
ThisBuild / version := "1.0.0-SNAPSHOT"
ThisBuild / organization := "com.gomezgimenez"

val javacppVersion = "1.5.4"

// Platform classifier for native library dependencies
val platform = org.bytedeco.javacpp.Loader.Detector.getPlatform

// JavaCPP-Preset libraries with native dependencies
val presetLibs = Seq(
  "opencv"   -> "4.4.0",
  "ffmpeg"   -> "4.3.1",
  "openblas" -> "0.3.10"
).flatMap { case (lib, ver) =>
  Seq(
    "org.bytedeco" % lib % s"$ver-$javacppVersion",
    "org.bytedeco" % lib % s"$ver-$javacppVersion" classifier platform
  )
}

lazy val `sla-printer-timelapse-tool` = (project in file("."))
  .settings(
    resolvers += "Clojars" at "https://clojars.org/repo/",
    mainClass in(Compile, run) := Some("Main"),
    mainClass in assembly := Some("Main"),
    libraryDependencies ++= List(
      json4s,
      scalaTest % Test,
      webcamCapture,
      scrImage,
      "org.bytedeco" % "javacv" % "1.5.4"
    ) ++ presetLibs
  )
