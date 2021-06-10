package com.gomezgimenez.timelapse.tool.component

import java.awt.image.BufferedImage
import java.util.UUID

import com.gomezgimenez.timelapse.tool.Util
import com.gomezgimenez.timelapse.tool.controller.Feature
import com.gomezgimenez.timelapse.tool.model.WebcamModel
import javafx.animation.AnimationTimer
import javafx.embed.swing.SwingFXUtils
import javafx.scene.canvas.Canvas
import javafx.scene.input.{ MouseButton, MouseEvent }
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import org.bytedeco.opencv.opencv_core.Point2f

case class TrackingPlotPanel(model: WebcamModel) extends Pane {

  class Timer extends AnimationTimer {
    override def handle(now: Long): Unit = draw()
  }

  new Timer().start()

  val canvas = new Canvas(getWidth, getHeight)
  getChildren.add(canvas)

  canvas.widthProperty().addListener(_ => draw())
  canvas.heightProperty().addListener(_ => draw())

  addEventHandler(
    MouseEvent.MOUSE_CLICKED,
    (e: MouseEvent) => {

      val img      = model.tracker.get().currentImage.get()
      val features = model.tracker.get().features.get()

      e match {
        case e: MouseEvent if e.getButton == MouseButton.PRIMARY =>
          val ratio      = getImageFitRatio(img)
          val marginTop  = (getHeight - img.getHeight * ratio) / 2
          val marginLeft = (getWidth - img.getWidth * ratio) / 2
          val point = new Point2f(
            ((e.getX - marginLeft) / ratio).toFloat,
            ((e.getY - marginTop) / ratio).toFloat
          )
          if (point.x >= 0 &&
              point.x < img.getWidth &&
              point.y >= 0 &&
              point.y < img.getHeight) {
            val feature = Feature(UUID.randomUUID().hashCode(), Some(point), model.featureSize.get())
            model.tracker.get().addFeature(feature)
          }
        case e: MouseEvent if e.getButton == MouseButton.SECONDARY =>
          val ratio      = getImageFitRatio(img)
          val marginTop  = (getHeight - img.getHeight * ratio) / 2
          val marginLeft = (getWidth - img.getWidth * ratio) / 2
          val point = new Point2f(
            ((e.getX - marginLeft) / ratio).toFloat,
            ((e.getY - marginTop) / ratio).toFloat
          )
          val nonSelectedFeatures =
            features.filterNot { f =>
              f.point.exists(
                p =>
                  point.x > (p.x - f.size / 2) && point.x < (p.x + f.size / 2) &&
                  point.y > (p.y - f.size / 2) && point.y < (p.y + f.size / 2))
            }
          nonSelectedFeatures.foreach { f =>
            model.tracker.get().removeFeature(f.id)
          }
      }
    }
  )

  override def layoutChildren(): Unit = {
    super.layoutChildren()
    canvas.setLayoutX(snappedLeftInset())
    canvas.setLayoutY(snappedTopInset())
    canvas.setWidth(snapSize(getWidth) - snappedLeftInset() - snappedRightInset())
    canvas.setHeight(snapSize(getHeight) - snappedTopInset() - snappedBottomInset())
  }

  private def draw(): Unit = {
    val g2d = canvas.getGraphicsContext2D
    g2d.clearRect(0, 0, getWidth, getHeight)

    val img      = model.tracker.get().currentImage.get()
    val features = model.tracker.get().features.get()
    val highMark = model.tracker.get().highMark.get()
    val lowMark  = model.tracker.get().lowMark.get()

    if (img != null) {
      val fxImg      = SwingFXUtils.toFXImage(img, null)
      val ratio      = getImageFitRatio(img)
      val marginTop  = (getHeight - img.getHeight * ratio) / 2
      val marginLeft = (getWidth - img.getWidth * ratio) / 2

      g2d.save()
      g2d.scale(ratio, ratio)
      g2d.drawImage(fxImg, marginLeft / ratio, marginTop / ratio)
      g2d.setStroke(Color.GRAY)
      g2d.strokeRect(marginLeft / ratio, marginTop / ratio, img.getWidth, img.getHeight)

      val heightTrackerSize = 15

      val massCenter = Util.massCenter(features.toList)
      massCenter.foreach { m =>
        g2d.setStroke(Color.CYAN.brighter())
        g2d.strokeOval(marginLeft / ratio + m.x - heightTrackerSize / 2, marginTop / ratio + m.y - heightTrackerSize / 2, heightTrackerSize, heightTrackerSize)
      }

      features.foreach { feature =>
        feature.point.foreach { f =>
          val x = marginLeft / ratio + f.x
          val y = marginTop / ratio + f.y
          g2d.setStroke(Color.RED)
          g2d.strokeRect(x - feature.size / 2, y - feature.size / 2, feature.size, feature.size)
          g2d.strokeText(feature.id.toString, x - feature.size / 2, y - feature.size / 2 - 4)
        }
      }

      highMark.foreach { p =>
        g2d.setStroke(Color.BLUE.brighter())
        g2d.strokeOval(marginLeft / ratio + p.x - heightTrackerSize / 2, marginTop / ratio + p.y - heightTrackerSize / 2, heightTrackerSize, heightTrackerSize)
      }
      lowMark.foreach { p =>
        g2d.setStroke(Color.GREEN.brighter())
        g2d.strokeOval(marginLeft / ratio + p.x - heightTrackerSize / 2, marginTop / ratio + p.y - heightTrackerSize / 2, heightTrackerSize, heightTrackerSize)
      }

      g2d.restore()
    }
  }

  def getImageFitRatio(img: BufferedImage): Double = {
    val widthRatio  = getWidth / img.getWidth
    val heightRatio = getHeight / img.getHeight
    Math.min(widthRatio, heightRatio)
  }

}
