package com.gomezgimenez.timelapse.tool.model

import com.gomezgimenez.timelapse.tool.controller.Point
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.Image

case class WebcamModel() {
  val source_image: SimpleObjectProperty[Image] = new SimpleObjectProperty[Image]()
  val reference_image: SimpleObjectProperty[Image] = new SimpleObjectProperty[Image]()
  val feature: SimpleObjectProperty[Option[Point]] = new SimpleObjectProperty[Option[Point]](None)
  val highFeature: SimpleObjectProperty[Option[Point]] = new SimpleObjectProperty[Option[Point]](None)
  val lowFeature: SimpleObjectProperty[Option[Point]] = new SimpleObjectProperty[Option[Point]](None)

}
