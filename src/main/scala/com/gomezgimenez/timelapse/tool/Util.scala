package com.gomezgimenez.timelapse.tool

import java.awt.image.BufferedImage

import com.gomezgimenez.timelapse.tool.controller.Feature
import org.bytedeco.javacpp.indexer.FloatIndexer
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.opencv_core._

object Util {

  def toMatPoint2f(points: Seq[Point2f]): Mat = {
    // Create Mat representing a vector of Points3f
    val dest = new Mat(1, points.size, CV_32FC2)
    val indx = dest.createIndexer().asInstanceOf[FloatIndexer]
    for (i <- points.indices) {
      val p = points(i)
      indx.put(0, i, 0, p.x)
      indx.put(0, i, 1, p.y)
    }
    require(dest.checkVector(2) >= 0)
    dest
  }

  def toPoint2fArray(mat: Mat): Array[Point2f] = {
    require(mat.checkVector(2) >= 0, "Expecting a vector Mat")

    val indexer = mat.createIndexer().asInstanceOf[FloatIndexer]
    val size = mat.total.toInt
    val dest = new Array[Point2f](size)

    for (i <- 0 until size) dest(i) = new Point2f(indexer.get(0, i, 0), indexer.get(0, i, 1))
    dest
  }

  def scaleFitWidth(img: BufferedImage, width: Int): BufferedImage = {
    import java.awt.image.BufferedImage
    val ratio = img.getHeight().toDouble / img.getWidth()
    val newImage = new BufferedImage(width, (width * ratio).toInt, BufferedImage.TYPE_INT_RGB)

    val g = newImage.createGraphics
    g.drawImage(img, 0, 0, width, (width * ratio).toInt, null)
    g.dispose()

    newImage
  }

  def massCenter(features: List[Feature]): Point2f = {
    val points =
      features
        .collect { case Feature(_, Some(point), _) => point }
    val sumPoint =
    points
      .foldLeft(new Point2f(0.0f, 0.0f))((acc, n) => new Point2f(acc.x + n.x, acc.y + n.y))
    new Point2f(sumPoint.x / points.length, sumPoint.y / points.length)
  }
}