import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.security.PrivateKey;

public class ClientGUI extends Application {
    private final int APP_WIDTH = 285;
    private final int APP_HEIGHT = 250;
    private Stage mainStage;
    private Stage clientStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Client client = new Client(this);
        FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("login.fxml"));
        Parent root = loginLoader.load();
        mainStage = primaryStage;
        primaryStage.setTitle("");
        primaryStage.setScene(new Scene(root, APP_WIDTH, APP_HEIGHT));
        primaryStage.setResizable(false);
        LoginGUIController loginGUIController = loginLoader.getController();
        loginGUIController.setClient(client);
        client.setLoginController(loginGUIController);

        FXMLLoader clientLoader = new FXMLLoader(getClass().getResource("client.fxml"));
        Parent clientRoot = clientLoader.load();
        Scene scene = new Scene(clientRoot);
        clientStage = new Stage();
        clientStage.setScene(scene);
        clientStage.setOnHidden(e -> {
            client.disconnect();
        });
        ClientGUIController clientGUIController = clientLoader.getController();
        clientGUIController.setClient(client);
        client.setClientController(clientGUIController);

        primaryStage.show();
    }

    void showClient() {
        Platform.runLater(() -> {
                clientStage.show();
        });
    }

    void closeLoginForm() {
        Platform.runLater(() -> {
            mainStage.close();
        });
    }

}
