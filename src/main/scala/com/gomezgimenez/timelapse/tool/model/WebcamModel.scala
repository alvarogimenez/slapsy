package com.gomezgimenez.timelapse.tool.model

import java.awt.image.BufferedImage

import com.gomezgimenez.timelapse.tool.controller.{BufferImage, Feature, WebCamSource}
import javafx.beans.property.{IntegerProperty, ObjectProperty, SimpleIntegerProperty, SimpleListProperty, SimpleObjectProperty}
import javafx.collections.FXCollections
import javafx.scene.image.Image
import org.bytedeco.opencv.opencv_core.Point2f

case class WebcamModel() {
  val features: SimpleListProperty[Feature] = new SimpleListProperty[Feature](FXCollections.observableArrayList[Feature])
  val availableCameras: SimpleListProperty[WebCamSource] = new SimpleListProperty[WebCamSource](FXCollections.observableArrayList[WebCamSource])
  val selectedCamera: SimpleObjectProperty[WebCamSource] = new SimpleObjectProperty[WebCamSource]()
  val sourceImage: SimpleObjectProperty[Image] = new SimpleObjectProperty[Image]()
  val playerImage: SimpleObjectProperty[Image] = new SimpleObjectProperty[Image]()
  val highMark: SimpleObjectProperty[Option[Point2f]] = new SimpleObjectProperty[Option[Point2f]](None)
  val lowMark: SimpleObjectProperty[Option[Point2f]] = new SimpleObjectProperty[Option[Point2f]](None)

  val recordingBuffer: SimpleObjectProperty[List[BufferImage]] = new SimpleObjectProperty[List[BufferImage]](List.empty)
  val currentFrame: SimpleIntegerProperty = new SimpleIntegerProperty(0)
  val fps: SimpleIntegerProperty = new SimpleIntegerProperty(30)
  val fpsObj: ObjectProperty[Integer] = fps.asObject()
}
