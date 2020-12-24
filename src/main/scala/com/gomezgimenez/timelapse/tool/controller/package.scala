package com.gomezgimenez.timelapse.tool

import java.awt.image.BufferedImage
import java.util.UUID

import org.bytedeco.opencv.opencv_core.Point2f

package object controller {

  case class ImageWithPosition(img: BufferedImage, hrImg: BufferedImage, p: Option[Point2f])

  case class WebCamSource(
                           name: String,
                           index: Int
                         ) {
    override def toString(): String = name
  }

  case class Feature(id: Int, point: Point2f, size: Float) {
    override def toString: String =
      s"$id: (${point.x},${point.y})"
  }

}
