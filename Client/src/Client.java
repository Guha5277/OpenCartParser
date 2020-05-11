import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Client extends Application{
    private final int APP_WIDTH = 300;
    private final int APP_HEIGHT = 250;


        @Override
        public void start(Stage primaryStage) throws Exception {
            Parent root = FXMLLoader.load(getClass().getResource("login.fxml"));
            primaryStage.setTitle("");
            primaryStage.setScene(new Scene(root, APP_WIDTH, APP_HEIGHT));
            primaryStage.setResizable(false);
            primaryStage.show();
        }

        public static void main(String[] args) {
            launch(args);
        }

}
