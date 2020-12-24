package com.gomezgimenez.timelapse.tool.controller

import java.awt.Dimension
import java.awt.image.BufferedImage

import com.github.sarxos.webcam.Webcam
import com.gomezgimenez.timelapse.tool.Util
import com.gomezgimenez.timelapse.tool.model.WebcamModel
import javafx.application.Platform
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.concurrent.Task
import javafx.embed.swing.SwingFXUtils
import javafx.fxml.FXML
import javafx.scene.control.{ComboBox, ListView}
import javafx.scene.image.Image
import javafx.scene.layout.BorderPane
import org.bytedeco.javacv.Java2DFrameUtils

import scala.collection.mutable
import scala.jdk.CollectionConverters._

case class MainWindowController(model: WebcamModel) {
  @FXML var main_panel: BorderPane = _
  @FXML var combo_camera: ComboBox[WebCamSource] = _
  @FXML var feature_list: ListView[Feature] = _

  var currentTask: Task[Unit] = _

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
    model.selectedCamera.addListener(new ChangeListener[WebCamSource] {
      override def changed(observable: ObservableValue[_ <: WebCamSource], oldValue: WebCamSource, newValue: WebCamSource): Unit = {
        run()
      }
    })

    run()
  }

  def run(): Unit = {
    if(currentTask != null) {
      currentTask.cancel()
    }
    currentTask = webCamTask()
    val thread = new Thread(currentTask)
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

        val maxBufferSize = 5
        val imageBuffer = mutable.Queue.empty[TrackingSnapshot]
        var raisingEdge = true

        while (!isCancelled) {
          val hrImg: BufferedImage = cam.getImage

          if (hrImg != null) {
            val img = Util.scaleFitWidth(hrImg, 1024)
            img.flush()

            Platform.runLater(() => {
              val newFeatures =
                imageBuffer.lastOption.map { lastImage =>
                  model.features.get.asScala.toList.map { feature =>
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

                    val newTrackedPoint =
                      feature.point.flatMap { p =>
                        calcOpticalFlowPyrLK(
                          source,
                          dest,
                          toMatPoint2f(Seq(p)),
                          trackedPointsNewUnfilteredMat,
                          trackingStatus,
                          err
                        )

                        toPoint2fArray(trackedPointsNewUnfilteredMat)
                          .headOption
                          .flatMap {
                            case p if p.x > 0 && p.x < img.getWidth && p.y > 0 && p.y < img.getHeight => Some(p)
                            case _ => None
                          }
                      }

                    feature.copy(point = newTrackedPoint)
                  }
                }

              imageBuffer.enqueue(TrackingSnapshot(img, hrImg, newFeatures.getOrElse(List.empty)))
              if (imageBuffer.size > maxBufferSize) {
                imageBuffer.dequeue()

                val dSteps =
                  (imageBuffer.toList match {
                    case a :: b :: tail => tail.foldLeft(List(a -> b)) {
                      case (acc, n) => acc :+ (acc.last._2 ->n)
                    }
                  }).map { case (t1, t2) =>
                    Util.massCenter(t2.features).y - Util.massCenter(t1.features).y
                  }

                val dAvg = dSteps.sum / dSteps.length
                if(raisingEdge && dAvg > 0) {
                  model.highMark.set(Some(Util.massCenter(imageBuffer.head.features)))
                  raisingEdge = false
                } else if(!raisingEdge && dAvg < 0) {
                  model.lowMark.set(Some(Util.massCenter(imageBuffer.head.features)))
                  raisingEdge = true
                }
              }
              newFeatures.foreach { nf =>
                model.features.get().setAll(nf.asJava)
              }
              model.sourceImage.set(SwingFXUtils.toFXImage(img, null))
            })
          }
        }

        cam.close()

        Platform.runLater(() => {
          model.sourceImage.set(null)
        })
      }
      catch {
        case e: Exception =>
          e.printStackTrace()
      }
    }
  }

  private def clone(img: Image): Image = {
    SwingFXUtils.toFXImage(SwingFXUtils.fromFXImage(img, null), null)
  }
}

