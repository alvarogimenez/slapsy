package com.gomezgimenez.timelapse.tool

import java.awt.Color
import java.awt.image.BufferedImage

package object controller {
  case class ImageWithPosition(img: BufferedImage, p: Option[Point])

  case class Point(x: Int, y: Int)

  def findNearest(
                           src: BufferedImage,
                           dst: BufferedImage,
                           feature: Point,
                           boxSize: Int
                         ): Option[Point] = {
    val travelDistance = 10
    ((feature.x - travelDistance) to (feature.x + travelDistance)).flatMap { px =>
      ((feature.y - travelDistance) to (feature.y + travelDistance)).map { py =>
        val diff =
          (-boxSize to boxSize).flatMap { dx =>
            (-boxSize to boxSize).map { dy =>
              val srcPixel = new Color(src.getRGB(feature.x - dx, feature.y - dy))
              val dstPixel = new Color(dst.getRGB(px - dx, py - dy))
              Math.abs(
                (srcPixel.getRed + srcPixel.getGreen + srcPixel.getBlue) / 3 -
                  (dstPixel.getRed + dstPixel.getGreen + dstPixel.getBlue) / 3)
            }
          }.sum

        Point(px, py) -> diff
      }
    }
      .sortBy(_._2)
      .collectFirst {
        case (point, diff)  => point
      }
  }
}
