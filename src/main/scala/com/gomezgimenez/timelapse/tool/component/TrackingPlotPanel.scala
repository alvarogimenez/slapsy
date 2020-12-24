package com.gomezgimenez.timelapse.tool.component

import com.gomezgimenez.timelapse.tool.Util
import com.gomezgimenez.timelapse.tool.controller.Feature
import com.gomezgimenez.timelapse.tool.model.WebcamModel
import javafx.event.EventType
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.input.{MouseButton, MouseEvent}
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import org.bytedeco.opencv.opencv_core.Point2f

import scala.jdk.CollectionConverters._

case class TrackingPlotPanel(model: WebcamModel) extends Pane {

  val canvas = new Canvas(getWidth, getHeight)
  getChildren.add(canvas)

  canvas.widthProperty().addListener(_ => draw())
  canvas.heightProperty().addListener(_ => draw())

  model.sourceImage.addListener(_ => draw())

  addEventHandler(MouseEvent.MOUSE_CLICKED,(e: MouseEvent) => e match {
    case e: MouseEvent if e.getButton == MouseButton.PRIMARY =>
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
        model.features.get().add(Feature(model.features.get().asScala.map(_.id).toSet.maxOption.getOrElse(0) + 1, Some(point), 20.0f))
      }
    case e:MouseEvent if e.getButton == MouseButton.SECONDARY =>
      val img = model.sourceImage.get
      val ratio = getImageFitRatio(img)
      val marginTop = (getHeight - img.getHeight * ratio) / 2
      val marginLeft = (getWidth - img.getWidth * ratio) / 2
      val point = new Point2f(
        ((e.getX - marginLeft) / ratio).toFloat,
        ((e.getY - marginTop) / ratio).toFloat
      )
      val nonSelectedFeatures =
        model.features.get().asScala.filterNot { f =>
        f.point.exists(p =>
          point.x > (p.x - f.size/2) && point.x < (p.x + f.size/2) &&
            point.y > (p.y - f.size/2) && point.y < (p.y + f.size/2))
      }
      model.features.get().setAll(nonSelectedFeatures.asJava)
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

      val heightTrackerSize = 15

      val massCenter = Util.massCenter(model.features.get().asScala.toList)
      g2d.setStroke(Color.CYAN.brighter())
      g2d.strokeOval(
        marginLeft / ratio + massCenter.x - heightTrackerSize/2,
        marginTop / ratio + massCenter.y - heightTrackerSize/2,
        heightTrackerSize, heightTrackerSize)

      model.features.get().asScala.foreach { feature =>
        feature.point.foreach { f =>
          val x = marginLeft / ratio + f.x
          val y = marginTop / ratio + f.y
          g2d.setStroke(Color.RED)
          g2d.strokeRect(x - feature.size/2, y - feature.size/2, feature.size, feature.size)
          g2d.strokeText(feature.id.toString, x - feature.size/2, y - feature.size/2 - 4)
        }
      }

      model.highMark.get.foreach { p =>
        g2d.setStroke(Color.BLUE.brighter())
        g2d.strokeOval(marginLeft / ratio + p.x - heightTrackerSize/2,
          marginTop / ratio + p.y - heightTrackerSize/2,
          heightTrackerSize, heightTrackerSize)
      }
      model.lowMark.get.foreach { p =>
        g2d.setStroke(Color.GREEN.brighter())
        g2d.strokeOval(marginLeft / ratio + p.x - heightTrackerSize/2,
          marginTop / ratio + p.y - heightTrackerSize/2,
          heightTrackerSize, heightTrackerSize)
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
