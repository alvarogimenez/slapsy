package com.gomezgimenez.timelapse.tool.component

import com.gomezgimenez.timelapse.tool.Util
import com.gomezgimenez.timelapse.tool.controller.Feature
import com.gomezgimenez.timelapse.tool.model.WebcamModel
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.input.{MouseButton, MouseEvent}
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import org.bytedeco.opencv.opencv_core.Point2f

import scala.jdk.CollectionConverters._

case class PlayerPlotPanel(model: WebcamModel) extends Pane {

  val canvas = new Canvas(getWidth, getHeight)
  getChildren.add(canvas)

  canvas.widthProperty().addListener(_ => draw())
  canvas.heightProperty().addListener(_ => draw())

  model.playerImage.addListener(_ => draw())


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

    if (model.playerImage.get != null) {
      val img = model.playerImage.get
      val ratio = getImageFitRatio(img)
      val marginTop = (getHeight - img.getHeight * ratio) / 2
      val marginLeft = (getWidth - img.getWidth * ratio) / 2

      g2d.save()
      g2d.scale(ratio, ratio)
      g2d.drawImage(img, marginLeft / ratio, marginTop / ratio)
      g2d.restore()
    }
  }

  def getImageFitRatio(img: Image): Double = {
    val widthRatio = getWidth / img.getWidth
    val heightRatio = getHeight / img.getHeight
    Math.min(widthRatio, heightRatio)
  }
}
