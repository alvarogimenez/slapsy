package com.gomezgimenez.timelapse.tool

import java.awt.image.BufferedImage

import org.bytedeco.opencv.opencv_core.Point2f

package object controller {

  case class TrackingSnapshot(img: BufferedImage, hrImg: BufferedImage, features: List[Feature])

  case class WebCamSource(
    name: String,
    index: Int
  ) {
    override def toString(): String = name
  }

  case class Feature(id: Int, point: Option[Point2f], size: Float) {
    override def toString: String =
      s"$id: (${point.map(_.x).getOrElse("-")},${point.map(_.y).getOrElse("-")})"

    override def equals(obj: Any): Boolean = obj match {
      case o: Feature => o.id == id
      case _ => false
    }
  }

}
