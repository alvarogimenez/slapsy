package com.gomezgimenez.timelapse.tool.model

import java.awt.image.BufferedImage

import com.gomezgimenez.timelapse.tool.threads.Tracker
import com.gomezgimenez.timelapse.tool.controller._
import javafx.beans.property._
import javafx.collections.FXCollections
import javafx.scene.image.Image

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

  val playerImage: SimpleObjectProperty[BufferedImage]               = new SimpleObjectProperty[BufferedImage]()
  val recordingBuffer: SimpleObjectProperty[List[ImgBufferRegister]] = new SimpleObjectProperty[List[ImgBufferRegister]](List.empty)
  val currentFrame: SimpleIntegerProperty                            = new SimpleIntegerProperty(0)
  val fps: SimpleIntegerProperty                                     = new SimpleIntegerProperty(0)
  val fpsObj: ObjectProperty[Integer]                                = fps.asObject()
}
