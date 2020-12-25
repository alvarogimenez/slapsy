package com.gomezgimenez.timelapse.tool.controller

import java.awt.Dimension
import java.awt.image.BufferedImage
import java.io.{BufferedWriter, File, FileWriter}

import com.github.sarxos.webcam.Webcam
import com.gomezgimenez.timelapse.tool.Util
import com.gomezgimenez.timelapse.tool.component.{PlayerPlotPanel, TrackingPlotPanel}
import com.gomezgimenez.timelapse.tool.model.WebcamModel
import javafx.application.Platform
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.concurrent.Task
import javafx.embed.swing.SwingFXUtils
import javafx.fxml.FXML
import javafx.scene.control._
import javafx.scene.layout.BorderPane
import org.bytedeco.javacv.Java2DFrameUtils
import javafx.scene.control.SpinnerValueFactory
import javafx.stage.FileChooser.ExtensionFilter
import javafx.stage.{DirectoryChooser, Stage}
import javax.imageio.ImageIO

import scala.collection.mutable
import scala.jdk.CollectionConverters._

case class MainWindowController(primaryStage: Stage, model: WebcamModel) {
  @FXML var main_panel: BorderPane = _
  @FXML var player_panel: BorderPane = _
  @FXML var combo_camera: ComboBox[WebCamSource] = _
  @FXML var feature_list: ListView[Feature] = _
  @FXML var play_button: Button = _
  @FXML var stop_button: Button = _
  @FXML var play_slider: Slider = _
  @FXML var current_frame_label: Label = _
  @FXML var max_frame_count_label: Label = _
  @FXML var fps_spinner: Spinner[Integer] = _

  @FXML var menu_file_export_frames: MenuItem = _
  @FXML var menu_file_close: MenuItem = _
  @FXML var menu_help_about: MenuItem = _

  var currentTrackingTask: Task[Unit] = _
  var currentPlayerTask: Task[Unit] = _
  var playing: Boolean = false

  def initialize(): Unit = {
    main_panel.setCenter(TrackingPlotPanel(model))
    player_panel.setCenter(PlayerPlotPanel(model))

    menu_file_close.setOnAction(_ => {
      Platform.exit()
    })
    menu_file_export_frames.setOnAction(_ => {
      val file = new File(".")
      val fileChooser = new DirectoryChooser
      fileChooser.setInitialDirectory(file.getParentFile)
      fileChooser.setTitle("Export all frames")
      val selectedFile = fileChooser.showDialog(primaryStage)
      if (selectedFile != null) {
        model.recordingBuffer.get().zipWithIndex.foreach { case(frame, index)=>
          ImageIO.write(frame.hrImg, "JPG", new File(selectedFile.getPath + "/" + s"Frame$index.jpg"))
        }
      }
    })

    play_button.setOnAction(_ => {
      play()
    })
    stop_button.setOnAction(_ => {
      stop()
    })

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

    fps_spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 20))
    fps_spinner.getValueFactory.valueProperty().bindBidirectional(model.fpsObj)

    play_slider.setMax(0)
    play_slider.valueProperty().bindBidirectional(model.currentFrame)

    model.recordingBuffer.addListener(new ChangeListener[List[BufferImage]] {
      override def changed(observable: ObservableValue[_ <: List[BufferImage]], oldValue: List[BufferImage], newValue: List[BufferImage]): Unit = {
        if(model.currentFrame.get == 0 && newValue.nonEmpty) {
          model.currentFrame.set(1)
          play_slider.setMin(1)
        }
        max_frame_count_label.setText(newValue.length.toString)
        play_slider.setMax(newValue.length)
      }
    })

    model.currentFrame.addListener(new ChangeListener[Number] {
      override def changed(observable: ObservableValue[_ <: Number], oldValue: Number, newValue: Number): Unit = {
        if(newValue.intValue() <= model.recordingBuffer.get().length && !playing) {
          model.playerImage.set(SwingFXUtils.toFXImage(model.recordingBuffer.get()(newValue.intValue() - 1).img, null))
        }
      }
    })

    current_frame_label.textProperty().bind(model.currentFrame.asString())

    run()
  }

  def stop(): Unit = {
    if(currentPlayerTask != null) {
      currentPlayerTask.cancel()
      playing = false
      model.currentFrame.set(1)
    }
  }

  def play(): Unit = {
    stop()
    currentPlayerTask = playTask()
    val thread = new Thread(currentPlayerTask)
    thread.setDaemon(true)
    thread.start()
  }

  def run(): Unit = {
    if(currentTrackingTask != null) {
      currentTrackingTask.cancel()
    }
    currentTrackingTask = webCamTask()
    val thread = new Thread(currentTrackingTask)
    thread.setDaemon(true)
    thread.start()
  }

  def playTask(): Task[Unit] = new Task[Unit]() {
    override def call(): Unit = {
      playing = true
      val frames = model.recordingBuffer.get()
      val delay = 1000 / model.fps.get
      frames.zipWithIndex.foreach { case (frame, index) =>
        Platform.runLater(() => {
          model.playerImage.set(SwingFXUtils.toFXImage(frame.img, null))
          model.currentFrame.set(index + 1)
        })
        Thread.sleep(delay)
      }
      playing = false
    }
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
                  model.recordingBuffer.set(model.recordingBuffer.get() :+ BufferImage(imageBuffer.head.img, imageBuffer.head.hrImg))
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
}

