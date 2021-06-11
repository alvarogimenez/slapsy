package com.gomezgimenez.timelapse.tool.settings

import java.io.{ BufferedWriter, File, FileWriter }

import com.gomezgimenez.timelapse.tool.controller.{ CustomResolution, FixedResolution, Resolution }
import com.gomezgimenez.timelapse.tool.model.WebcamModel
import io.circe.{ Decoder, Encoder, Printer }
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.extras.auto._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder

import scala.io.Source

object SettingsService {
  implicit val genDevConfig: Configuration =
    Configuration.default
      .withDiscriminator("type")

  implicit val ResolutionEncoder: Encoder[SettingsResolution] = deriveConfiguredEncoder
  implicit val ResolutionDecoder: Decoder[SettingsResolution] = deriveConfiguredDecoder

  def loadSettings: Option[Settings] = {
    val configFile = new File("./settings.config")
    if (configFile.exists()) {
      val source        = Source.fromFile(configFile)
      val fileContents  = source.mkString
      val configuration = decode[Settings](fileContents).toOption
      source.close
      configuration
    } else {
      None
    }
  }

  def saveSettings(settings: Settings): Unit = {
    val configFile = new File("./settings.config")
    val bw         = new BufferedWriter(new FileWriter(configFile))
    val json       = settings.asJson.dropNullValues.spaces2
    bw.write(json)
    bw.close()
  }

  def fromModel(model: WebcamModel): Settings =
    Settings(
      version = "1.0",
      trackingSettings = Some(
        TrackingSettings(
          camera = model.selectedCamera.get.name,
          resolution = model.selectedResolution.get match {
            case CustomResolution =>
              SettingsCustomResolution(
                width = model.customResolutionWidth.get,
                height = model.customResolutionHeight.get
              )
            case FixedResolution(width, height) =>
              SettingsFixedResolution(width, height)
          }
        )
      )
    )

}
