package com.gomezgimenez.timelapse.tool.model

import java.awt.image.BufferedImage

import com.gomezgimenez.timelapse.tool.controller.{Feature, WebCamSource}
import javafx.beans.property.{SimpleListProperty, SimpleObjectProperty}
import javafx.collections.FXCollections
import javafx.scene.image.Image
import org.bytedeco.opencv.opencv_core.Point2f

case class WebcamModel() {
  val features: SimpleListProperty[Feature] = new SimpleListProperty[Feature](FXCollections.observableArrayList[Feature])
  val availableCameras: SimpleListProperty[WebCamSource] = new SimpleListProperty[WebCamSource](FXCollections.observableArrayList[WebCamSource])
  val selectedCamera: SimpleObjectProperty[WebCamSource] = new SimpleObjectProperty[WebCamSource]()
  val sourceImage: SimpleObjectProperty[Image] = new SimpleObjectProperty[Image]()
  val highMark: SimpleObjectProperty[Option[Point2f]] = new SimpleObjectProperty[Option[Point2f]](None)
  val lowMark: SimpleObjectProperty[Option[Point2f]] = new SimpleObjectProperty[Option[Point2f]](None)

  val recordingBuffer: SimpleObjectProperty[List[BufferedImage]] = new SimpleObjectProperty[List[BufferedImage]](List.empty)
}
