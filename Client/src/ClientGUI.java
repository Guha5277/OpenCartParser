import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientGUI extends Application {
    private final int APP_WIDTH = 285;
    private final int APP_HEIGHT = 250;
    private Stage mainStage;
    private Stage clientStage;
    private Stage productFilterStage;
    private ProductFilterGuiController productFilterController;

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
        Scene clientScene = new Scene(clientRoot);
        clientStage = new Stage();
        clientStage.setScene(clientScene);
        clientStage.setOnHidden(e -> {
            client.disconnect();
        });
        ClientGUIController clientGUIController = clientLoader.getController();
        clientGUIController.setClient(client);
        client.setClientController(clientGUIController);

        FXMLLoader filterLoader = new FXMLLoader((getClass().getResource("product_filter.fxml")));
        Parent filterRoot = filterLoader.load();
        Scene filterScene = new Scene(filterRoot);
        productFilterController = filterLoader.getController();
        productFilterController.setClient(client);
        productFilterStage = new Stage();
        productFilterStage.setScene(filterScene);

        primaryStage.show();
    }

    void showClient() {
        Platform.runLater(() -> {
                clientStage.show();
        });
    }

    void showProductFilter(boolean inStockSelected, ObservableList<String> cityList, int selectedCity, ObservableList<String> storeList, int selectedStore) {
        Platform.runLater(() -> {
            productFilterStage.show();
            productFilterController.setStockParam(inStockSelected, cityList, selectedCity, storeList, selectedStore);
        });
    }

    void closeLoginStage() {
        Platform.runLater(() -> {
            mainStage.close();
        });
    }

    void closeClientStage() {
        Platform.runLater(() -> {
            clientStage.close();
        });
    }


}
