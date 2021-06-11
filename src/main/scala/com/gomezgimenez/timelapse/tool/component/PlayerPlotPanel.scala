package com.gomezgimenez.timelapse.tool.component

import com.gomezgimenez.timelapse.tool.model.WebcamModel
import javafx.embed.swing.SwingFXUtils
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.layout.Pane
import javafx.scene.paint.Color

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
      val img        = SwingFXUtils.toFXImage(model.playerImage.get, null)
      val ratio      = getImageFitRatio(img)
      val marginTop  = (getHeight - img.getHeight * ratio) / 2
      val marginLeft = (getWidth - img.getWidth * ratio) / 2

      g2d.save()
      g2d.scale(ratio, ratio)
      g2d.drawImage(img, marginLeft / ratio, marginTop / ratio)
      g2d.setStroke(Color.GRAY)
      g2d.strokeRect(marginLeft / ratio, marginTop / ratio, img.getWidth, img.getHeight)
      g2d.restore()
    }
  }

  def getImageFitRatio(img: Image): Double = {
    val widthRatio  = getWidth / img.getWidth
    val heightRatio = getHeight / img.getHeight
    Math.min(widthRatio, heightRatio)
  }
}
