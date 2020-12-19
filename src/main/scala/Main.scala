import com.gomezgimenez.timelapse.tool.controller
import com.gomezgimenez.timelapse.tool.controller.MainWindowController
import com.gomezgimenez.timelapse.tool.model.WebcamModel
import javafx.application.{Application, Platform}
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.BorderPane
import javafx.stage.Stage

object Main {
  def main(args: Array[String]): Unit = {
    Application.launch(classOf[Main], args:_*)
  }
}

class Main extends Application {

  override def start(primaryStage: Stage): Unit = {
    val webcamModel = WebcamModel()
    Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA)

    val loader = new FXMLLoader()
    loader.setControllerFactory(_ => controller.MainWindowController(webcamModel))
    loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/MainWindow.fxml"))

    val rootLayout = loader.load().asInstanceOf[BorderPane]
    val scene = new Scene(rootLayout, 1024, 768)
    primaryStage.setScene(scene)
    primaryStage.setTitle("SLA Printer Timelapse Tool - Álvaro Gómez Giménez")
    primaryStage.setMinWidth(scene.getWidth)
    primaryStage.setMinHeight(scene.getHeight)
    primaryStage.setOnCloseRequest(_ => {
      Platform.exit()
    })
    primaryStage.show()
  }
}


