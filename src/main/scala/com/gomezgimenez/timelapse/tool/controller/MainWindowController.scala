package com.gomezgimenez.timelapse.tool.controller

import java.awt.Dimension
import java.awt.image.BufferedImage
import java.io.File

import com.github.sarxos.webcam.Webcam
import com.gomezgimenez.timelapse.tool.threads.{ Tracker, TrackerListener }
import com.gomezgimenez.timelapse.tool.component.{ PlayerPlotPanel, TrackingPlotPanel }
import com.gomezgimenez.timelapse.tool.model.WebcamModel
import javafx.application.Platform
import javafx.beans.value.{ ChangeListener, ObservableValue }
import javafx.concurrent.Task
import javafx.embed.swing.SwingFXUtils
import javafx.fxml.FXML
import javafx.scene.control.{ SpinnerValueFactory, _ }
import javafx.scene.layout.BorderPane
import javafx.stage.{ DirectoryChooser, Stage }
import javafx.util.converter.NumberStringConverter
import javax.imageio.ImageIO

import scala.jdk.CollectionConverters._

case class MainWindowController(primaryStage: Stage, model: WebcamModel) {
  @FXML var main_panel: BorderPane                           = _
  @FXML var player_panel: BorderPane                         = _
  @FXML var feature_size_spinner: Spinner[Integer]           = _
  @FXML var combo_camera: ComboBox[WebCamSource]             = _
  @FXML var combo_resolution: ComboBox[Resolution]           = _
  @FXML var feature_list: ListView[Feature]                  = _
  @FXML var play_button: Button                              = _
  @FXML var stop_button: Button                              = _
  @FXML var play_slider: Slider                              = _
  @FXML var current_frame_label: Label                       = _
  @FXML var max_frame_count_label: Label                     = _
  @FXML var fps_spinner: Spinner[Integer]                    = _
  @FXML var restart_capture_button: Button                   = _
  @FXML var stop_capture_button: Button                      = _
  @FXML var width_input: TextField                           = _
  @FXML var height_input: TextField                          = _
  @FXML var export_file_prefix_input: TextField              = _
  @FXML var export_file_type_combo: ComboBox[ExportFileType] = _
  @FXML var current_com_speed_label: Label                   = _
  @FXML var max_com_speed_label: Label                       = _
  @FXML var current_fps_label: Label                         = _

  @FXML var menu_file_export_frames: MenuItem = _
  @FXML var menu_file_close: MenuItem         = _
  @FXML var menu_help_about: MenuItem         = _

  var currentPlayerTask: Task[Unit] = _
  var playing: Boolean              = false

  def initialize(): Unit = {
    main_panel.setCenter(TrackingPlotPanel(model))
    player_panel.setCenter(PlayerPlotPanel(model))

    menu_file_close.setOnAction(_ => {
      Platform.exit()
    })
    menu_file_export_frames.setOnAction(_ => {
      exportFrames()
    })

    play_button.setOnAction(_ => {
      play()
    })
    stop_button.setOnAction(_ => {
      stop()
    })

    val availableCameras: List[WebCamSource] =
      Webcam.getWebcams.asScala.zipWithIndex.map {
        case (w, index) => WebCamSource(w.getName, index)
      }.toList
    model.availableCameras.get().setAll(availableCameras.asJava)
    combo_camera.itemsProperty().bindBidirectional(model.availableCameras)
    combo_camera.valueProperty().bindBidirectional(model.selectedCamera)

    model.selectedCamera.set(availableCameras.head)

    model.availableResolutions
      .get()
      .setAll(
        List(
          FixedResolution(640, 480),
          FixedResolution(800, 600),
          FixedResolution(1027, 768),
          FixedResolution(1920, 1080),
          CustomResolution
        ).asJava
      )
    model.selectedResolution.set(FixedResolution(1920, 1080))
    combo_resolution
      .itemsProperty()
      .bindBidirectional(model.availableResolutions)
    combo_resolution.valueProperty().bindBidirectional(model.selectedResolution)
    width_input
      .textProperty()
      .bindBidirectional(
        model.customResolutionWidth,
        new NumberStringConverter("#")
      )
    height_input
      .textProperty()
      .bindBidirectional(
        model.customResolutionHeight,
        new NumberStringConverter("#")
      )
    width_input
      .disableProperty()
      .bind(combo_resolution.valueProperty().isEqualTo(CustomResolution).not())
    height_input
      .disableProperty()
      .bind(combo_resolution.valueProperty().isEqualTo(CustomResolution).not())

    restart_capture_button.setOnAction(_ => {
      runTracking()
    })
    stop_capture_button.setOnAction(_ => {
      stopTracking()
    })

    feature_list.itemsProperty().bind(model.features)

    feature_size_spinner.setValueFactory(
      new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 30)
    )
    feature_size_spinner.getValueFactory
      .valueProperty()
      .bindBidirectional(model.featureSizeObj)

    fps_spinner.setValueFactory(
      new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 20)
    )
    fps_spinner.getValueFactory.valueProperty().bindBidirectional(model.fpsObj)

    play_slider.setMax(0)
    play_slider.valueProperty().bindBidirectional(model.currentFrame)

    model.recordingBuffer.addListener(new ChangeListener[List[BufferedImage]] {
      override def changed(observable: ObservableValue[_ <: List[BufferedImage]], oldValue: List[BufferedImage], newValue: List[BufferedImage]): Unit = {
        if (model.currentFrame.get == 0 && newValue.nonEmpty) {
          model.currentFrame.set(1)
          play_slider.setMin(1)
        }
        max_frame_count_label.setText(newValue.length.toString)
        play_slider.setMax(newValue.length)
      }
    })

    model.currentFrame.addListener(new ChangeListener[Number] {
      override def changed(observable: ObservableValue[_ <: Number], oldValue: Number, newValue: Number): Unit =
        if (newValue.intValue() <= model.recordingBuffer.get().length && !playing) {
          model.playerImage.set(model.recordingBuffer.get()(newValue.intValue() - 1))
        }
    })

    current_frame_label.textProperty().bind(model.currentFrame.asString())

    model.availableExportFileTypes
      .get()
      .setAll(
        List(
          ExportFileType("JPG", "JPG"),
          ExportFileType("PNG", "PNG"),
          ExportFileType("BMP", "BMP")
        ).asJava
      )
    model.selectedExportFileType.set(
      model.availableExportFileTypes.get.asScala.head
    )
    export_file_type_combo
      .itemsProperty()
      .bindBidirectional(model.availableExportFileTypes)
    export_file_type_combo
      .valueProperty()
      .bindBidirectional(model.selectedExportFileType)

    export_file_prefix_input
      .textProperty()
      .bindBidirectional(model.exportFilePrefix)

    runTracking()
  }

  def stop(): Unit =
    if (currentPlayerTask != null) {
      currentPlayerTask.cancel()
      playing = false
      model.currentFrame.set(1)
    }

  def play(): Unit = {
    stop()
    currentPlayerTask = playTask()
    val thread = new Thread(currentPlayerTask)
    thread.setDaemon(true)
    thread.start()
  }

  def stopTracking(): Unit =
    if (model.tracker.get != null) {
      model.tracker.get.cancel()
    }

  def runTracking(): Unit = {
    stopTracking()
    val prefSize = model.selectedResolution.get() match {
      case FixedResolution(w, h) => new Dimension(w, h)
      case CustomResolution      => new Dimension(model.customResolutionWidth.get, model.customResolutionHeight.get)
    }
    val tracker = new Tracker(
      model.selectedCamera.get,
      prefSize,
      24,
      new TrackerListener {
        def invalidate(t: Tracker) =
          Platform.runLater(() => {
            current_fps_label.setText(t.currentFps.get.toString)
            model.features.setAll(t.features.get().asJava)
          })
        def capture(img: BufferedImage): Unit =
          Platform.runLater(() => {
            model.recordingBuffer.set(model.recordingBuffer.get() :+ img)
          })
      }
    )
    model.tracker.set(tracker)
    val thread = new Thread(tracker)
    thread.setDaemon(true)
    thread.start()
  }

  def playTask(): Task[Unit] = new Task[Unit]() {
    override def call(): Unit = {
      playing = true
      val frames = model.recordingBuffer.get()
      val delay  = 1000 / model.fps.get
      frames.zipWithIndex.foreach {
        case (frame, index) =>
          Platform.runLater(() => {
            model.playerImage.set(frame)
            model.currentFrame.set(index + 1)
          })
          Thread.sleep(delay)
      }
      playing = false
    }
  }

  def exportFrames(): Unit = {
    val file        = new File(".")
    val fileChooser = new DirectoryChooser
    fileChooser.setInitialDirectory(file.getParentFile)
    fileChooser.setTitle("Export all frames")
    val selectedFile = fileChooser.showDialog(primaryStage)
    if (selectedFile != null) {
      val fileType   = model.selectedExportFileType.get.code
      val filePrefix = model.exportFilePrefix.get
      model.recordingBuffer.get().zipWithIndex.foreach {
        case (frame, index) =>
          ImageIO.write(
            frame,
            fileType,
            new File(
              selectedFile.getPath + "/" + filePrefix + s"$index.${fileType.toLowerCase}"
            )
          )
      }
    }
  }
}
