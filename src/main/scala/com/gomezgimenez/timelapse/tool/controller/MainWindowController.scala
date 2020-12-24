package com.gomezgimenez.timelapse.tool.controller

import java.awt.Dimension
import java.awt.image.BufferedImage
import java.io.File

import com.github.sarxos.webcam.Webcam
import com.gomezgimenez.timelapse.tool.Util
import com.gomezgimenez.timelapse.tool.model.WebcamModel
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.embed.swing.SwingFXUtils
import javafx.fxml.FXML
import javafx.scene.control.{Button, ComboBox, ListView}
import javafx.scene.image.Image
import javafx.scene.layout.BorderPane
import javax.imageio.ImageIO
import org.bytedeco.javacv.Java2DFrameUtils
import org.bytedeco.opencv.opencv_core.Point2f

import scala.collection.mutable
import scala.jdk.CollectionConverters._

case class MainWindowController(model: WebcamModel) {
  @FXML var main_panel: BorderPane = _
  @FXML var combo_camera: ComboBox[WebCamSource] = _
  @FXML var feature_list: ListView[Feature] = _

  def initialize(): Unit = {
    main_panel.setCenter(PlotPanel(model))

    val availableCameras: List[WebCamSource] =
        Webcam.getWebcams.asScala
          .zipWithIndex.map { case (w, index) => WebCamSource(w.getName, index) }
            .toList
    model.availableCameras.get().setAll(availableCameras.asJava)
    combo_camera.itemsProperty().bindBidirectional(model.availableCameras)
    combo_camera.valueProperty().bindBidirectional(model.selectedCamera)

    feature_list.itemsProperty().bind(model.features)

    model.selectedCamera.set(availableCameras.head)

    val thread = new Thread(webCamTask())
    thread.setDaemon(true)
    thread.start()
  }

  def webCamTask(): Task[Unit] = new Task[Unit]() {

    override def call(): Unit = {
      try {
        val cam = Webcam.getWebcams.get(model.selectedCamera.get().index)
        if (!cam.isOpen) {
          cam.setCustomViewSizes(Array(new Dimension(1920, 1080)))
          cam.setViewSize(new Dimension(1920, 1080))
          cam.open()
        }

        val maxBufferSize = 60
        val derivativeStep = 5
        val imageBuffer = mutable.Queue.empty[ImageWithPosition]
        var raisingEdge = true

        model.feature.set(Some(new Point2f(320.0f, 240.0f)))

        while (!isCancelled) {
          val hrImg: BufferedImage = cam.getImage

          if (hrImg != null) {
            val img = Util.scaleFitWidth(hrImg, 1024)
            imageBuffer.lastOption.foreach { lastImage =>
              if(model.feature.get.isDefined) {
                import com.gomezgimenez.timelapse.tool.Util._
                import org.bytedeco.opencv.global.opencv_imgproc._
                import org.bytedeco.opencv.global.opencv_video._
                import org.bytedeco.opencv.opencv_core._
                val sourceImgMat = Java2DFrameUtils.toMat(lastImage.img)
                val destImgMat = Java2DFrameUtils.toMat(img)
                val source = new Mat()
                val dest = new Mat()

                cvtColor(sourceImgMat, source, COLOR_BGRA2GRAY)
                cvtColor(destImgMat, dest, COLOR_BGRA2GRAY)

                val trackingStatus = new Mat()
                val trackedPointsNewUnfilteredMat = new Mat()
                val err = new Mat()

                calcOpticalFlowPyrLK(
                  source,
                  dest,
                  toMatPoint2f(Seq(model.feature.get.get)),
                  trackedPointsNewUnfilteredMat,
                  trackingStatus,
                  err
                )

                val trackedPointsNewUnfiltered = toPoint2fArray(trackedPointsNewUnfilteredMat)
                trackedPointsNewUnfiltered.headOption.map { p =>
                  model.feature.set(Some(p))
                }
              }
            }

            imageBuffer.enqueue(ImageWithPosition(img, hrImg, model.feature.get))
            if(imageBuffer.size > maxBufferSize) {
              imageBuffer.dequeue()
            }
            if (imageBuffer.size > derivativeStep) {
              val img1 = imageBuffer.last
              val img2 = imageBuffer.dropRight(derivativeStep).last
              for {
                f1 <- img1.p
                f2 <- img2.p
              } yield {
                val dy = f2.y - f1.y
                if (raisingEdge && dy < -1) {
                  model.highFeature.set(Some(f2))
                  model.recordingBuffer.set(model.recordingBuffer.get :+ img)
                  ImageIO.write(img2.hrImg, "jpg", new File(s"target/${System.currentTimeMillis()}.jpg"))
                  raisingEdge = false
                } else if (!raisingEdge && dy > 0) {
                  raisingEdge = true
                  model.lowFeature.set(Some(f2))
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

