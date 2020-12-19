package com.gomezgimenez.timelapse.tool.controller

import java.awt.Dimension
import java.awt.image.BufferedImage

import com.github.sarxos.webcam.Webcam
import com.gomezgimenez.timelapse.tool.model.WebcamModel
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.embed.swing.SwingFXUtils
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.image.Image
import javafx.scene.layout.BorderPane

import scala.collection.mutable


case class MainWindowController(model: WebcamModel) {
  @FXML var capture_button: Button = _
  @FXML var main_panel: BorderPane = _

  def initialize(): Unit = {

    capture_button.setOnAction(_ => {
      model.reference_image.set(clone(model.source_image.get))
    })

    main_panel.setCenter(PlotPanel(model))

    val thread = new Thread(webCamTask(1))
    thread.setDaemon(true)
    thread.start()
  }

  def webCamTask(index: Int): Task[Unit] = new Task[Unit]() {

    override def call(): Unit = {
      try {
        val cam = Webcam.getWebcams.get(index)
        if (!cam.isOpen) {
          cam.setViewSize(new Dimension(640, 480))
          cam.open()
        }

        val maxBufferSize = 60
        val derivativeStep = 5
        val imageBuffer = mutable.Queue.empty[ImageWithPosition]
        var raisingEdge = true

        model.feature.set(Some(Point(320, 240)))

        while (!isCancelled) {
          val img: BufferedImage = cam.getImage

          if (img != null) {
            imageBuffer.lastOption.foreach { lastImage =>
              if(model.feature.get.isDefined) {
                model.feature.set(findNearest(lastImage.img, img, model.feature.get.get, 10))
              }
            }

            imageBuffer.enqueue(ImageWithPosition(img, model.feature.get))
            if(imageBuffer.size > maxBufferSize) {
              imageBuffer.dequeue()
            }
            if(imageBuffer.size > derivativeStep) {
              for {
                p1 <- imageBuffer.last.p
                p2 <- imageBuffer.dropRight(derivativeStep).last.p
              } yield {
                val dy = p2.y - p1.y
                if(raisingEdge && dy <0) {
                  model.highFeature.set(Some(p1))
                  raisingEdge = false
                } else if(!raisingEdge && dy > 0) {
                  raisingEdge = true
                  model.lowFeature.set(Some(p1))
                }
              }
            }

            img.flush()

            Platform.runLater(() => {
              model.source_image.set(SwingFXUtils.toFXImage(img, null))
            })
          }
        }

        cam.close()
        Platform.runLater(() => {
          model.source_image.set(null)
        })

      } catch {
        case e: Exception =>
          e.printStackTrace()
      }
    }
  }

  private def clone(img: Image): Image = {
    SwingFXUtils.toFXImage(SwingFXUtils.fromFXImage(img, null), null)
  }
}

