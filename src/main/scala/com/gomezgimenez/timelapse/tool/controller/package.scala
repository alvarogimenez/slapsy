package com.gomezgimenez.timelapse.tool

import java.awt.image.BufferedImage
import java.util.Locale

import org.bytedeco.opencv.opencv_core.Point2f

package object controller {

  case class BufferImage(img: BufferedImage, hrImg: BufferedImage)

  case class TrackingSnapshot(img: BufferedImage, hrImg: BufferedImage, features: List[Feature])

  case class WebCamSource(
    name: String,
    index: Int
  ) {
    override def toString(): String = name
  }

  case class Feature(id: Int, point: Option[Point2f], size: Float) {
    override def toString: String =
      s"$id: (${point.map(p => String.format(Locale.US, "%.2f", p.x)).getOrElse("-")}," +
        s"${point.map(p => String.format(Locale.US, "%.2f", p.y)).getOrElse("-")})"

    override def equals(obj: Any): Boolean = obj match {
      case o: Feature => o.id == id
      case _ => false
    }
  }

  sealed trait Resolution
  case object CustomResolution extends Resolution{
    override def toString: String = "Custom..."
  }
  case class FixedResolution(width: Int, height: Int) extends Resolution {
    override def toString: String = s"${width}x$height"
  }

  case class ExportFileType(label: String, code: String) {
    override def toString: String = label
  }

}
