package com.gomezgimenez.timelapse.tool.model

import java.awt.image.BufferedImage

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.Image
import org.bytedeco.opencv.opencv_core.Point2f

case class WebcamModel() {
  val source_image: SimpleObjectProperty[Image] = new SimpleObjectProperty[Image]()
  val reference_image: SimpleObjectProperty[Image] = new SimpleObjectProperty[Image]()
  val feature: SimpleObjectProperty[Option[Point2f]] = new SimpleObjectProperty[Option[Point2f]](None)
  val highFeature: SimpleObjectProperty[Option[Point2f]] = new SimpleObjectProperty[Option[Point2f]](None)
  val lowFeature: SimpleObjectProperty[Option[Point2f]] = new SimpleObjectProperty[Option[Point2f]](None)

  val recordingBuffer: SimpleObjectProperty[List[BufferedImage]] = new SimpleObjectProperty[List[BufferedImage]](List.empty)
}
