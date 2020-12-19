package com.gomezgimenez.timelapse.tool.controller

import com.gomezgimenez.timelapse.tool.model.WebcamModel
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{Border, BorderStroke, BorderStrokeStyle, Pane}
import javafx.scene.paint.Color

case class PlotPanel(model: WebcamModel) extends Pane {

  val canvas = new Canvas(getWidth, getHeight)
  getChildren.add(canvas)
  canvas.widthProperty().addListener(_ => draw())
  canvas.heightProperty().addListener(_ => draw())

  model.source_image.addListener(_ => draw())

  addEventHandler(MouseEvent.MOUSE_CLICKED, (e: MouseEvent) => {
    val img = model.source_image.get
    val ratio = getImageFitRatio(img)
    val marginTop = (getHeight - img.getHeight*ratio)/2
    val marginLeft = (getWidth - img.getWidth*ratio)/2
    val feature = Point(
      x = ((e.getX - marginLeft)/ratio).toInt,
      y = ((e.getY - marginTop)/ratio).toInt
    )
    if(feature.x >= 0 && feature.x < img.getWidth &&
    feature.y >= 0 && feature.y < img.getHeight) {
      model.feature.set(Some(feature))
    } else {
      model.feature.set(None)
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

    if(model.source_image.get != null) {
      val img = model.source_image.get
      val ratio = getImageFitRatio(img)
      val marginTop = (getHeight - img.getHeight*ratio)/2
      val marginLeft = (getWidth - img.getWidth*ratio)/2

      g2d.save()
      g2d.scale(ratio, ratio)
      g2d.drawImage(img, marginLeft/ratio, marginTop/ratio)
      model.feature.get.foreach { f =>
        g2d.setStroke(Color.RED)
        g2d.strokeRect(marginLeft/ratio + f.x - 10, marginTop/ratio + f.y - 10, 20, 20)
      }
      model.highFeature.get.foreach { f =>
        g2d.setStroke(Color.BLUE)
        g2d.strokeRect(marginLeft/ratio + f.x - 10, marginTop/ratio + f.y - 10, 20, 20)
      }
      model.lowFeature.get.foreach { f =>
        g2d.setStroke(Color.GREEN)
        g2d.strokeRect(marginLeft/ratio + f.x - 10, marginTop/ratio + f.y - 10, 20, 20)
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
