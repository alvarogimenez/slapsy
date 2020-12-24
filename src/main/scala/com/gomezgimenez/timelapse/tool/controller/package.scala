package com.gomezgimenez.timelapse.tool

import java.awt.image.BufferedImage

import org.bytedeco.opencv.opencv_core.Point2f

package object controller {
  case class ImageWithPosition(img: BufferedImage, hrImg: BufferedImage, p: Option[Point2f])
}
