import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientGUI extends Application {
    private final int APP_WIDTH = 285;
    private final int APP_HEIGHT = 250;
    private Stage mainStage;
    private Stage clientStage;
    private Stage productFilterStage;
    private ProductFilterGuiController productFilterController;
    private static final Logger LOGGER = LogManager.getLogger("ClientLogger");

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        LOGGER.info("Building GUI...");
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
        LOGGER.info("Login Stage built successful");

        FXMLLoader clientLoader = new FXMLLoader(getClass().getResource("client.fxml"));
        Parent clientRoot = clientLoader.load();
        Scene clientScene = new Scene(clientRoot);
        clientStage = new Stage();
        clientStage.setScene(clientScene);
        clientStage.setOnHidden(e -> {
            LOGGER.info("Hide client stage");
            client.disconnect();
        });
        ClientGUIController clientGUIController = clientLoader.getController();
        clientGUIController.setClient(client);
        client.setClientController(clientGUIController);
        LOGGER.info("Client Stage built successful");

        FXMLLoader filterLoader = new FXMLLoader((getClass().getResource("product_filter.fxml")));
        Parent filterRoot = filterLoader.load();
        Scene filterScene = new Scene(filterRoot);
        productFilterController = filterLoader.getController();
        productFilterController.setClient(client);
        productFilterStage = new Stage();
        productFilterStage.setScene(filterScene);

        primaryStage.show();
        LOGGER.info("Show login stage...");
    }

    void showClient() {
        LOGGER.info("Show client stage...");
        Platform.runLater(() -> {
                clientStage.show();
        });
    }

    void showProductFilter(boolean inStockSelected, ObservableList<String> cityList, int selectedCity, ObservableList<String> storeList, int selectedStore) {
        LOGGER.info("Show products filter...");
        Platform.runLater(() -> {
            productFilterStage.show();
            productFilterController.setStockParam(inStockSelected, cityList, selectedCity, storeList, selectedStore);
        });
    }

    void hideLoginStage() {
        LOGGER.info("Hide login stage...");
        Platform.runLater(() -> {
            mainStage.hide();
        });
    }

    void showLoginStage() {
        LOGGER.info("Show login stage...");
        Platform.runLater(() -> {
            mainStage.show();
        });
    }

    void hideClientStage() {
        LOGGER.info("Hide client stage...");
        Platform.runLater(() -> {
            clientStage.hide();
        });
    }
}
