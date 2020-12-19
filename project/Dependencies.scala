import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.2"
  lazy val json4s = "org.json4s" %% "json4s-native" % "3.7.0-M7"
  lazy val webcamCapture ="com.github.sarxos" % "webcam-capture" % "0.3.11"
  lazy val scrImage ="com.sksamuel.scrimage" %% "scrimage-scala" % "4.0.11"
  lazy val openCv = "org.bytedeco" % "javacv-platform" % "1.5.4"
}
