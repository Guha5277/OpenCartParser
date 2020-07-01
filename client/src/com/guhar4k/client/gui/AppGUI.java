package com.guhar4k.client.gui;

import com.guhar4k.client.core.ControllerEvents;
import com.guhar4k.client.core.Controller;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import com.guhar4k.product.Product;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalTime;
import java.util.List;


public class AppGUI extends Application implements ControllerEvents {
    private final int APP_WIDTH = 285;
    private final int APP_HEIGHT = 250;
    private Stage loginStage;
    private Stage clientStage;
    private Stage productFilterStage;
    private Stage settingsStage;
    private Stage imageStage;
    private ProductFilterGUI productFilterController;
    private SettingsGUI settingsController;
    private ImageTestGUI imageSceneController;
    private static final Logger LOGGER = LogManager.getLogger("ClientLogger");

    private GUIEvents listener;
    private LoginGUI loginController;
    private ClientGUI clientController;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        listener = new Controller(this);

        LOGGER.info("Building GUI...");
        FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("login.fxml"));
        Parent root = loginLoader.load();
        loginStage = primaryStage;
        primaryStage.setTitle("");
        primaryStage.setScene(new Scene(root, APP_WIDTH, APP_HEIGHT));
        primaryStage.setResizable(false);
        loginController = loginLoader.getController();
        loginController.setListener(listener);
        LOGGER.info("Login Stage built successful");

        FXMLLoader clientLoader = new FXMLLoader(getClass().getResource("client.fxml"));
        Parent clientRoot = clientLoader.load();
        Scene clientScene = new Scene(clientRoot);
        clientStage = new Stage();
        clientStage.setScene(clientScene);
        clientStage.setOnHidden(e -> {
            LOGGER.info("Hide controller stage");
            listener.onAppCloseRequest();
        });

        clientController = clientLoader.getController();
        clientController.setListener(listener);
        clientController.setApp(this);
        LOGGER.info("Client Stage built successful");

        FXMLLoader filterLoader = new FXMLLoader((getClass().getResource("product_filter.fxml")));
        Parent filterRoot = filterLoader.load();
        Scene filterScene = new Scene(filterRoot);
        productFilterController = filterLoader.getController();
        productFilterController.setListener(listener);
        productFilterStage = new Stage();
        productFilterStage.setScene(filterScene);
        productFilterStage.setResizable(false);
        LOGGER.info("Filter Stage built successful");

        FXMLLoader imagesLoader = new FXMLLoader((getClass().getResource("image_test.fxml")));
        Parent imagesRoot = imagesLoader.load();
        Scene imagesScene = new Scene(imagesRoot);
        imageSceneController = imagesLoader.getController();
        imageStage = new Stage();
        imageStage.setScene(imagesScene);
        imageStage.setResizable(false);
        LOGGER.info("Images Test Stage built successful");

        FXMLLoader settingsLoader = new FXMLLoader((getClass().getResource("settings.fxml")));
        Parent settingsRoot = settingsLoader.load();
        Scene settingsScene = new Scene(settingsRoot);
        settingsController = settingsLoader.getController();
        settingsController.setListener(listener);
        settingsStage = new Stage();
        settingsStage.setScene(settingsScene);
        settingsStage.setResizable(false);
        settingsStage.setTitle("Настройки");
        settingsStage.getIcons().add(new Image("icon_settings_black.png"));

        LOGGER.info("Settings Stage built successful");
        listener.onGUIReady();
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
        productFilterStage.show();
        productFilterController.setStockParam(inStockSelected, cityList, selectedCity, storeList, selectedStore);
    }

    void hideLoginStage() {
        LOGGER.info("Hide login stage...");
        Platform.runLater(() -> {
            loginStage.hide();
        });
    }

    void showLoginStage() {
        LOGGER.info("Show login stage...");
        Platform.runLater(() -> {
            loginStage.show();
        });
    }

    void showSettingsStage() {
        LOGGER.info("Show settings stage...");
        settingsStage.show();
    }

    void showImagesStage() {
        LOGGER.info("Show image stage...");
        Platform.runLater(() -> {
            imageStage.show();
        });
    }

    void hideClientStage() {
        LOGGER.info("Hide controller stage...");
        Platform.runLater(() -> {
            clientStage.hide();
        });
    }

    //Model events
    @Override
    public void onLoginConfigLoaded(String ip, String port, boolean saveSetState, String login, String password) {
        loginController.onConfigLoaded(ip, port, saveSetState, login, password);
        Platform.runLater(() -> {
            loginStage.show();
        });
    }

    @Override
    public void onConnectFailed(String message) {
        Platform.runLater(() -> {
            loginController.connectionFailed(message);
        });
    }

    @Override
    public void onAuthAccepted(String nickname) {
        Platform.runLater(() -> {
            clientController.setNickname(nickname);
            loginController.authAccepted();
        });
    }

    @Override
    public void onAuthDenied() {
        Platform.runLater(() -> {
            loginController.authDenied();
        });
    }

    @Override
    public void onMultiplySession(String nickname) {
        Platform.runLater(() -> {
            loginController.multiplySession(nickname);
        });
    }

    @Override
    public void onFailToGetServerInfo() {
        Platform.runLater(() -> {
            loginController.failedToGetData();
        });
    }

    @Override
    public void onServerInfoReceived(boolean isAdmin, String startTime, int productsCount, int warehousesCount, int clientsCount) {
        Platform.runLater(() -> {
            loginStage.hide();
            loginController.setDisableAll(false);

            clientController.setTabsEnable(isAdmin);
            clientController.setServerUpTime(startTime);
            clientController.setProductsCount(String.valueOf(productsCount));
            clientController.setWarehousesCount(String.valueOf(warehousesCount));
            clientController.setActiveUsersCount(String.valueOf(clientsCount));

            clientStage.show();
        });
    }

    @Override
    public void onUpdaterInfoReceived(String lastRunDate, boolean autostart, int interval, LocalTime autostartTime, boolean hasLastUpdatedPosition) {
        Platform.runLater(() -> {
            clientController.setUpdaterLastRun(lastRunDate);
            clientController.setLastPositionCheckboxVisible(hasLastUpdatedPosition);
            settingsController.setUpdaterAutostartState(autostart);
            settingsController.setUpdaterDaysInterval(interval);
            settingsController.setUpdaterAutostartTime(autostartTime);
        });
    }

    @Override
    public void onResearcherInfoReceived(String lastRunDate, boolean autostart, int interval, LocalTime autostartTime) {
        Platform.runLater(() -> {
            clientController.setResearcherLastUpdate(lastRunDate);
            settingsController.setResearcherAutostartState(autostart);
            settingsController.setResearcherDaysInterval(interval);
            settingsController.setResearcherAutostartTime(autostartTime);
        });
    }

    @Override
    public void onActiveUsersCountChanged(String count) {
        Platform.runLater(() -> {
            clientController.setActiveUsersCount(count);
        });
    }

    @Override
    public void onUsersListUpdated(String[] users) {
        Platform.runLater(() -> {
            clientController.updateUsersList(users);
        });
    }

    @Override
    public void onKickUserFailed(String userNicknameToKick) {
        Platform.runLater(() -> {
            clientController.failedToKickUser(userNicknameToKick);
        });
    }

    @Override
    public void kickedFromTheServer(String initiator) {
        Platform.runLater(() -> {
            clientController.kickedFromTheServer(initiator);
            clientStage.hide();
            loginStage.show();
        });
    }

    @Override
    public void onReceiveCityName(String city) {
        Platform.runLater(() -> {
            clientController.addCityToComb(city);
        });
    }

    @Override
    public void onUpdaterStart() {
        Platform.runLater(() -> {
            clientController.updaterStart();
        });
    }

    @Override
    public void onUpdaterProgressChanged(double progress, String progressLabelText, String name) {
        Platform.runLater(() -> {
            clientController.setUpdaterProgress(progress, progressLabelText);
            clientController.setUpdaterCurrentProduct(name);
        });
    }

    @Override
    public void onUpdaterDifferencesFound(String count, String content) {
        Platform.runLater(() -> {
            clientController.setUpdatesFound(count);
            clientController.appendDifferencesFound(content);
        });
    }

    @Override
    public void updaterError(String URL, String failsCount) {
        Platform.runLater(() -> {
            clientController.setUpdatesFailed(failsCount);
        });
    }

    @Override
    public void onUpdaterException(String productID, String productURL, String message) {
        Platform.runLater(() -> {
            clientController.appendErrorToUpdaterLogger(productID, productURL, message);
        });
    }

    @Override
    public void onUpdaterException(String exception, String message) {
        Platform.runLater(() -> {
            clientController.appendErrorToUpdaterLogger(exception, message);
        });
    }

    @Override
    public void onUpdaterLastRunChanged(String lastRunDate) {
        Platform.runLater(() -> {
            clientController.setUpdaterLastRun(lastRunDate);
        });
    }

    @Override
    public void onUpdaterLastPositionChanged(boolean hasLastUpdatedPosition) {
        Platform.runLater(() -> {
            clientController.setLastPositionCheckboxVisible(hasLastUpdatedPosition);
        });
    }

    @Override
    public void updaterStopped() {
        Platform.runLater(() -> {
            clientController.updaterStopped();
        });
    }

    @Override
    public void onResearcherStart() {
        Platform.runLater(() -> {
            clientController.researcherStart();
        });
    }

    @Override
    public void onResearcherStopped() {
        Platform.runLater(() -> {
            clientController.researcherStopped();
        });
    }

    @Override
    public void onResearcherProgressChanged(double progress, String progressLabelText, String group) {
        Platform.runLater(() -> {
            clientController.setResearcherProgress(progress, progressLabelText);
            clientController.setResearcherCurrentGroup(group);
        });
    }

    @Override
    public void onResearcherCurrentCategoryChanged(String position, String categoryName) {
        Platform.runLater(() -> {
            clientController.setResearcherCurrentCategory(position, categoryName);
        });
    }

    @Override
    public void onResearcherNewProductFound(String product, String totalFound) {
        Platform.runLater(() -> {
            clientController.appendResearcherFoundProd(product);
            clientController.setResearcherTotalFounds(totalFound);
        });
    }

    @Override
    public void noSelectedProducts() {
        Platform.runLater(() -> {
            clientController.resetProductComboBoxes();
        });
    }

    @Override
    public void onProductRequestSent(boolean stock, String city, String store) {
        Platform.runLater(() -> {
            clientController.updateProductComboBoxes(stock, city, store);
            clientController.clearProductList();
        });
    }

    @Override
    public void onProductsNotFound() {
        Platform.runLater(() -> {
            clientController.productsNotFound();
        });
    }

    @Override
    public void onProductFound(Product product) {
        Platform.runLater(() -> {
            clientController.addToProductList(product);
        });
    }

    @Override
    public void allProductsReceived(List<Product> products) {
        Platform.runLater(() -> {
            //clientController.updateProductTableContent(products);
        });
    }

    @Override
    public void onProductImageFound(int productID, Image image) {
        Platform.runLater(() -> {
            boolean isShowing = imageStage.getScene().getWindow().isShowing();
            if (!isShowing){
                double x = clientStage.getX() + imageStage.getWidth();
                double y = clientStage.getY();

                imageStage.show();
                imageStage.setX(x);
                imageStage.setY(y);
            }
            imageSceneController.setImage(productID, image);
        });
    }

    @Override
    public void onProductImageNotFound(String productID) {
        Platform.runLater(() -> {
            imageStage.show();
            imageSceneController.noImageForProduct(productID);
        });
    }

    @Override
    public void onServerUptimeUpdated(String serverTime) {
        Platform.runLater(() -> {
            clientController.setServerUpTime(serverTime);
        });
    }

    @Override
    public void onConnectLost() {
        Platform.runLater(() -> {
            clientController.connectionLost();
            loginController.connectionFailed("Соединение потеряно");
            clientStage.hide();
            settingsStage.hide();
            imageStage.hide();
            loginStage.show();
        });
    }


}
