import sbt._

object Dependencies {
  lazy val javacppVersion = "1.5.4"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.2"
  lazy val json4s = "org.json4s" %% "json4s-native" % "3.7.0-M7"
  lazy val webcamCapture = "com.github.sarxos" % "webcam-capture" % "0.3.11"
  lazy val javacv = "org.bytedeco" % "javacv" % "1.5.4"

  // Platform classifier for native library dependencies
  lazy val platform = org.bytedeco.javacpp.Loader.Detector.getPlatform

  // JavaCPP-Preset libraries with native dependencies
  lazy val javaCvLibs = Seq(
    "opencv" -> "4.4.0",
    "ffmpeg" -> "4.3.1",
    "openblas" -> "0.3.10"
  ).flatMap { case (lib, ver) =>
    Seq(
      "org.bytedeco" % lib % s"$ver-$javacppVersion",
      "org.bytedeco" % lib % s"$ver-$javacppVersion" classifier platform
    )
  }
}
