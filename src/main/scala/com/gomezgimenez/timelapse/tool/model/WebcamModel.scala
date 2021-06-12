package com.gomezgimenez.timelapse.tool.model

import java.awt.image.BufferedImage

import com.gomezgimenez.timelapse.tool.controller._
import com.gomezgimenez.timelapse.tool.settings.{ Settings, SettingsCustomResolution, SettingsFixedResolution }
import com.gomezgimenez.timelapse.tool.threads.Tracker
import javafx.beans.property._
import javafx.collections.FXCollections
import scala.jdk.CollectionConverters._

case class WebcamModel() {
  val tracker: ObjectProperty[Tracker] = new SimpleObjectProperty[Tracker]()

  val features: SimpleListProperty[Feature]                        = new SimpleListProperty[Feature](FXCollections.observableArrayList[Feature])
  val featureSize: SimpleIntegerProperty                           = new SimpleIntegerProperty(30)
  val featureSizeObj: ObjectProperty[Integer]                      = featureSize.asObject()
  val availableCameras: SimpleListProperty[WebCamSource]           = new SimpleListProperty[WebCamSource](FXCollections.observableArrayList[WebCamSource])
  val selectedCamera: SimpleObjectProperty[WebCamSource]           = new SimpleObjectProperty[WebCamSource]()
  val availableResolutions: SimpleListProperty[Resolution]         = new SimpleListProperty[Resolution](FXCollections.observableArrayList[Resolution])
  val selectedResolution: SimpleObjectProperty[Resolution]         = new SimpleObjectProperty[Resolution]()
  val availableExportFileTypes: SimpleListProperty[ExportFileType] = new SimpleListProperty[ExportFileType](FXCollections.observableArrayList[ExportFileType])
  val selectedExportFileType: SimpleObjectProperty[ExportFileType] = new SimpleObjectProperty[ExportFileType]()
  val exportFilePrefix: SimpleStringProperty                       = new SimpleStringProperty("Frame_")
  val customResolutionWidth: SimpleIntegerProperty                 = new SimpleIntegerProperty(1920)
  val customResolutionHeight: SimpleIntegerProperty                = new SimpleIntegerProperty(1080)
  val outputDirectory: SimpleStringProperty                        = new SimpleStringProperty("target")

  val playerImage: SimpleObjectProperty[BufferedImage]             = new SimpleObjectProperty[BufferedImage]()
  val recordingBuffer: SimpleObjectProperty[List[CompressedImage]] = new SimpleObjectProperty[List[CompressedImage]](List.empty)
  val currentFrame: SimpleIntegerProperty                          = new SimpleIntegerProperty(0)
  val fps: SimpleIntegerProperty                                   = new SimpleIntegerProperty(24)
  val fpsObj: ObjectProperty[Integer]                              = fps.asObject()

  def updateFromSettings(settings: Settings): Unit =
    settings.trackingSettings.foreach { trackingSettings =>
      availableCameras.get.asScala.find(_.name == trackingSettings.camera).foreach { c =>
        selectedCamera.set(c)
      }
      trackingSettings.resolution match {
        case c: SettingsCustomResolution =>
          selectedResolution.set(CustomResolution)
          customResolutionWidth.set(c.width)
          customResolutionHeight.set(c.height)
        case f: SettingsFixedResolution if availableResolutions.contains(FixedResolution(f.width, f.height)) =>
          selectedResolution.set(FixedResolution(f.width, f.height))
      }
      featureSize.set(trackingSettings.featureSize)
      exportFilePrefix.set(trackingSettings.exportFilePrefix)
      availableExportFileTypes.get.asScala.find(_.code == trackingSettings.exportFileType).foreach { fileType =>
        selectedExportFileType.set(fileType)
      }
      outputDirectory.set(trackingSettings.exportDirectory)
    }
}
