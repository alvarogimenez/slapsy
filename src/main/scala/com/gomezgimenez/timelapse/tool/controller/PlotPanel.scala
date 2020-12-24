package com.gomezgimenez.timelapse.tool.controller

import java.util.UUID

import com.gomezgimenez.timelapse.tool.Util
import com.gomezgimenez.timelapse.tool.model.WebcamModel
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import org.bytedeco.opencv.opencv_core.Point2f

import scala.jdk.CollectionConverters._


case class PlotPanel(model: WebcamModel) extends Pane {

  val canvas = new Canvas(getWidth, getHeight)
  getChildren.add(canvas)

  canvas.widthProperty().addListener(_ => draw())
  canvas.heightProperty().addListener(_ => draw())

  model.sourceImage.addListener(_ => draw())

  addEventHandler(MouseEvent.MOUSE_CLICKED, (e: MouseEvent) => {
    val img = model.sourceImage.get
    val ratio = getImageFitRatio(img)
    val marginTop = (getHeight - img.getHeight * ratio) / 2
    val marginLeft = (getWidth - img.getWidth * ratio) / 2
    val point = new Point2f(
      ((e.getX - marginLeft) / ratio).toFloat,
      ((e.getY - marginTop) / ratio).toFloat
    )
    if (point.x >= 0 &&
      point.x < img.getWidth &&
      point.y >= 0 &&
      point.y < img.getHeight
    ) {
      model.features.get().add(Feature(model.features.size + 1, Some(point), 10.0f))
    }
  })

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

    if (model.sourceImage.get != null) {
      val img = model.sourceImage.get
      val ratio = getImageFitRatio(img)
      val marginTop = (getHeight - img.getHeight * ratio) / 2
      val marginLeft = (getWidth - img.getWidth * ratio) / 2

      g2d.save()
      g2d.scale(ratio, ratio)
      g2d.drawImage(img, marginLeft / ratio, marginTop / ratio)

      val massCenter = Util.massCenter(model.features.get().asScala.toList)
      g2d.setStroke(Color.CYAN.brighter())
      g2d.strokeRect(marginLeft / ratio + massCenter.x - 10, marginTop / ratio + massCenter.y - 10, 20, 20)

      model.features.get().asScala.foreach { feature =>
        feature.point.foreach { f =>
          g2d.setStroke(Color.RED)
          g2d.strokeRect(marginLeft / ratio + f.x - 10, marginTop / ratio + f.y - 10, 20, 20)
        }
      }

      model.highMark.get.foreach { p =>
        g2d.setStroke(Color.BLUE.brighter())
        g2d.strokeRect(marginLeft / ratio + p.x - 10, marginTop / ratio + p.y - 10, 20, 20)
      }
      model.lowMark.get.foreach { p =>
        g2d.setStroke(Color.GREEN.brighter())
        g2d.strokeRect(marginLeft / ratio + p.x - 10, marginTop / ratio + p.y - 10, 20, 20)
      }

      g2d.restore()
    }
  }

  def getImageFitRatio(img: Image): Double = {
    val widthRatio = getWidth / img.getWidth
    val heightRatio = getHeight / img.getHeight
    Math.min(widthRatio, heightRatio)
  }

}
