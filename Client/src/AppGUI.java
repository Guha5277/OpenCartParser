import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AppGUI extends Application {
    private final int APP_WIDTH = 285;
    private final int APP_HEIGHT = 250;
    private Stage mainStage;
    private Stage clientStage;
    private Stage productFilterStage;
    private Stage settingsStage;
    private ProductFilterGUI productFilterController;
    private SettingsGUI settingsController;
    private static final Logger LOGGER = LogManager.getLogger("ClientLogger");

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        LOGGER.info("Building GUI...");
        Controller controller = new Controller(this);
        FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("login.fxml"));
        Parent root = loginLoader.load();
        mainStage = primaryStage;
        primaryStage.setTitle("");
        primaryStage.setScene(new Scene(root, APP_WIDTH, APP_HEIGHT));
        primaryStage.setResizable(false);
        LoginGUI loginGUI = loginLoader.getController();
        loginGUI.setController(controller);
        controller.setLoginGUI(loginGUI);
        LOGGER.info("Login Stage built successful");

        FXMLLoader clientLoader = new FXMLLoader(getClass().getResource("client.fxml"));
        Parent clientRoot = clientLoader.load();
        Scene clientScene = new Scene(clientRoot);
        clientStage = new Stage();
        clientStage.setScene(clientScene);
        clientStage.setOnHidden(e -> {
            LOGGER.info("Hide controller stage");
            controller.disconnect();
        });
        ClientGUI clientGUI = clientLoader.getController();
        clientGUI.setController(controller);
        controller.setClientGUI(clientGUI);
        LOGGER.info("Client Stage built successful");

        FXMLLoader filterLoader = new FXMLLoader((getClass().getResource("product_filter.fxml")));
        Parent filterRoot = filterLoader.load();
        Scene filterScene = new Scene(filterRoot);
        productFilterController = filterLoader.getController();
        productFilterController.setController(controller);
        productFilterStage = new Stage();
        productFilterStage.setScene(filterScene);
        productFilterStage.setResizable(false);
        LOGGER.info("Filter Stage built successful");

        FXMLLoader settingsLoader = new FXMLLoader((getClass().getResource("settings.fxml")));
        Parent settingsRoot = settingsLoader.load();
        Scene settingsScene = new Scene(settingsRoot);
        settingsController = settingsLoader.getController();
        settingsController.setController(controller);
        settingsStage = new Stage();
        settingsStage.setScene(settingsScene);
        settingsStage.setResizable(false);
        settingsStage.setTitle("Настройки");
        settingsStage.getIcons().add(new Image("icon_settings_black.png"));
        controller.setSettingsGUI(settingsController);
        LOGGER.info("Settings Stage built successful");

        primaryStage.show();
        LOGGER.info("Show login stage...");
    }

    void showClient() {
        LOGGER.info("Show controller stage...");
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

    void showSettingsStage() {
        LOGGER.info("Show settings stage...");
        Platform.runLater(() -> {
            settingsStage.show();
        });
    }

    void hideClientStage() {
        LOGGER.info("Hide controller stage...");
        Platform.runLater(() -> {
            clientStage.hide();
        });
    }
}
