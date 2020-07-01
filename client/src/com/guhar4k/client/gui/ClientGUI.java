package com.guhar4k.client.gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import com.guhar4k.product.Product;
import com.guhar4k.product.Warehouse;

import java.util.List;
import java.util.Optional;

public class ClientGUI {
    private GUIEvents listener;
    private boolean isUpdaterRun;
    private boolean isResearcherRun;
    private int selectedProduct;
    private String prevProdSelectedItem;
    private AppGUI app;

    //Header
    @FXML
    private Label lblNickname;

    //Tabs
    @FXML
    private Tab tabUpdater;
    @FXML
    private Tab tabResearcher;
    @FXML
    private Tab tabProducts;
    @FXML
    private Tab tabUsers;
    @FXML
    private Tab tabGrabber;

    //Server Part
    @FXML
    private Label lblServerUpTime;
    @FXML
    private Label lblProductsCount;
    @FXML
    private Label lblWarehousesCount;
    @FXML
    private Label lblActiveUsers;
    @FXML
    private ListView<Label> updaterUsersListView;
    @FXML
    private Button btnBanUser;
    @FXML
    private Button btnKickUser;

    //Updater Part
    @FXML
    private Label lblUpdaterStatus;
    @FXML
    private CheckBox chkLastSavedPos;
    @FXML
    private Button btnUpdaterStart;
    @FXML
    private Button btnUpdaterStop;
    @FXML
    private Label lblUpdated;
    @FXML
    private Label lblUpdaterLastRunText;
    @FXML
    private Label lblUpdaterLastRunDate;
    @FXML
    private Label lblUpdateFailed;
    @FXML
    private Label lblUpdatesCount;
    @FXML
    private Label lblUpdateFailsCount;
    @FXML
    private Label lblUpdCurrentProd;
    @FXML
    private ProgressBar prgrUpdater;
    @FXML
    private Label lblProgress;
    @FXML
    private TextField fieldManualUpdate;
    @FXML
    private CheckBox chkUpdSwitchSource;
    @FXML
    private Button btnUpdManualUpdate;
    @FXML
    private TextArea updaterLogArea;

    //researcher
    @FXML
    private Label lblResearcherStatus;
    @FXML
    private Button btnResearcherStart;
    @FXML
    private Button btnResearcherStop;
    @FXML
    private Label lblResearcherLastRunText;
    @FXML
    private Label lblResearcherLastRunDate;
    @FXML
    private Label lblResearchFound;
    @FXML
    private Label lblResearchFailed;
    @FXML
    private Label lblResearcherCategory;
    @FXML
    private Label lblResearcherCategoryPos;
    @FXML
    private Label lblResearcherCategoryName;
    @FXML
    private Label lblResearcherCurrentGroupName;
    @FXML
    private Label lblResearcherGroupPos;
    @FXML
    private ProgressBar prgrResearcher;
    @FXML
    private TextArea researcherLogArea;

    //products part
    @FXML
    private ComboBox<String> combCity;
    @FXML
    private ComboBox<String> combStore;
    @FXML
    private CheckBox chkStock;
    @FXML
    private Button btnFilter;
    @FXML
    private Button btnShow;
    @FXML
    private TableView<Product> productTableView;
    @FXML
    private TableColumn<Product, Integer> colProdID;
    @FXML
    private TableColumn<Product, String> colProductName;
    @FXML
    private TableColumn<Product, Integer> colProductPrice;
    @FXML
    private TableColumn<Product, Integer> colProductStrength;
    @FXML
    private TableColumn<Product, Integer> colProductVolume;
    @FXML
    private TableColumn<Product, Integer> colProductCategory;
    @FXML
    private TableColumn<Product, Integer> colProductUrl;
    @FXML
    private Label totalProductShow;

    @FXML
    private TableView<Warehouse> remainsTableView;
    @FXML
    private TableColumn<?, ?> colRemainsMain;
    @FXML
    private TableColumn<Warehouse, Integer> colRemainsID;
    @FXML
    private TableColumn<Warehouse, String> colRemainsWarehouse;
    @FXML
    private TableColumn<Warehouse, Integer> colRemainsCount;
    ObservableList<Product> productsList;
    ObservableList<Warehouse> remainsList;

    @FXML
    void initialize() {
        combCity.getItems().add("Все города");
        combCity.getSelectionModel().select(0);
        combStore.getItems().add("Все магазины");
        combStore.getSelectionModel().select(0);

        //Set content for Products Column
        colProdID.setCellValueFactory(new PropertyValueFactory<>("id"));
        colProductName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colProductPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colProductStrength.setCellValueFactory(new PropertyValueFactory<>("strength"));
        colProductVolume.setCellValueFactory(new PropertyValueFactory<>("volume"));
        colProductCategory.setCellValueFactory(new PropertyValueFactory<>("categoryID"));
        colProductUrl.setCellValueFactory(new PropertyValueFactory<>("URL"));

        colRemainsID.setCellValueFactory(new PropertyValueFactory<>("id"));
        colRemainsWarehouse.setCellValueFactory(new PropertyValueFactory<>("altName"));
        colRemainsCount.setCellValueFactory(new PropertyValueFactory<>("remains"));

        //Set adaptive size for columns
        colProdID.prefWidthProperty().bind(productTableView.widthProperty().divide(12));
        colProductName.prefWidthProperty().bind(productTableView.widthProperty().divide(3));
        colProductPrice.prefWidthProperty().bind(productTableView.widthProperty().divide(12));
        colProductStrength.prefWidthProperty().bind(productTableView.widthProperty().divide(10));
        colProductVolume.prefWidthProperty().bind(productTableView.widthProperty().divide(11));
        colProductCategory.prefWidthProperty().bind(productTableView.widthProperty().divide(9));
        colProductUrl.prefWidthProperty().bind(productTableView.widthProperty().divide(5));

        colRemainsMain.prefWidthProperty().bind(remainsTableView.widthProperty());
        colRemainsID.prefWidthProperty().bind(remainsTableView.widthProperty().divide(4));
        colRemainsWarehouse.prefWidthProperty().bind(remainsTableView.widthProperty().divide(2));
        colRemainsCount.prefWidthProperty().bind(remainsTableView.widthProperty().divide(4));

        productTableView.setItems(productsList);
        remainsTableView.setVisible(false);

        //Row factory and detecting click on row in table
        productTableView.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY) {
                    Product product = row.getItem();
                    if (product.getRemainsCount() > 0) {
                        remainsTableView.setVisible(true);
                        String store = combStore.getSelectionModel().getSelectedItem();
                        List<Warehouse> list = product.getRemainsList();
                        if (!store.equals("Все магазины")) {
                            list.get(0).setAltName(store);
                        }
                        if (remainsList == null) {
                            remainsList = FXCollections.observableArrayList(list);
                        } else {
                            remainsList.removeAll(remainsList);
                            remainsList.addAll(list);
                        }

                        remainsTableView.setItems(remainsList);
                    } else {
                        remainsTableView.setVisible(false);
                    }
                    //get an image request
                    int id = product.getId();
                    if (id != selectedProduct) {
                        selectedProduct = id;
                        listener.getImageRequest(product.getId());
                    }
                }
            });

            return row;
        });
    }

    void setListener(GUIEvents listener) {
        this.listener = listener;
    }

    void setNickname(String nickname) {
        lblNickname.setText(nickname);
    }

    void setTabsEnable(boolean isAdmin) {
        tabUpdater.setDisable(false);
        tabResearcher.setDisable(false);
        tabProducts.setDisable(false);
        tabUsers.setDisable(!isAdmin);
        tabGrabber.setDisable(!isAdmin);
    }

    void setServerUpTime(String time) {
        lblServerUpTime.setText(time);
    }

    void setProductsCount(String count) {
        lblProductsCount.setText(count);
    }

    void setWarehousesCount(String count) {
        lblWarehousesCount.setText(count);
    }

    void setActiveUsersCount(String count) {
        lblActiveUsers.setText(count);
    }

    void updateUsersList(String[] users) {
        updaterUsersListView.setDisable(users.length == 1);
        updaterUsersListView.getItems().clear();
        String ownNick = lblNickname.getText();
        for (String nickname : users) {
            if (ownNick.equals(nickname)) continue;
            updaterUsersListView.getItems().add(new Label(nickname));
        }
    }

    //Updater
    void updaterStart() {
        showUpdaterElements();
    }

    void setUpdaterProgress(double progress, String progressText) {
        if (!isUpdaterRun) {
            isUpdaterRun = true;
            showUpdaterElements();
        }
        chkLastSavedPos.setDisable(true);
        prgrUpdater.setProgress(progress);
        lblProgress.setText(progressText);

    }

    void setLastPositionCheckboxVisible(boolean state) {
        chkLastSavedPos.setVisible(state);
        chkLastSavedPos.setDisable(isUpdaterRun);
    }

    void setUpdaterCurrentProduct(String productName) {
        if (!isUpdaterRun) {
            isUpdaterRun = true;
            showUpdaterElements();
        }
        lblUpdCurrentProd.setText(productName);

    }

    void setUpdatesFound(String count) {
        if (!isUpdaterRun) {
            isUpdaterRun = true;
            showUpdaterElements();
        }
        if (!lblUpdated.isVisible()) {
            lblUpdated.setVisible(true);
            lblUpdatesCount.setVisible(true);
        }
        lblUpdatesCount.setText(count);
    }

    void setUpdatesFailed(String count) {
        if (!isUpdaterRun) {
            isUpdaterRun = true;
            showUpdaterElements();
        }
        if (!lblUpdateFailed.isVisible()) {
            lblUpdateFailed.setVisible(true);
            lblUpdateFailsCount.setVisible(true);
        }
        lblUpdateFailsCount.setText(count);
    }

    void appendDifferencesFound(String diff) {
        if (updaterLogArea.isDisable()) updaterLogArea.setDisable(false);
        updaterLogArea.appendText(diff + "\n");
    }

    void appendErrorToUpdaterLogger(String reason, String exceptionMsg) {
        if (updaterLogArea.isDisable()) updaterLogArea.setDisable(false);
        updaterLogArea.appendText("Error: " + reason + ": " + exceptionMsg + "\n");
    }

    void appendErrorToUpdaterLogger(String id, String url, String exceptionMsg) {
        if (updaterLogArea.isDisable()) updaterLogArea.setDisable(false);
        updaterLogArea.appendText("Error of product with id: " + id + ", url: " + url + ", " + exceptionMsg + "\n");
    }

    void setUpdaterLastRun(String date) {
        lblUpdaterLastRunText.setVisible(true);
        lblUpdaterLastRunDate.setText(date);
        lblUpdaterLastRunDate.setVisible(true);
    }

    private void showUpdaterElements() {
        //updater status
        lblUpdaterStatus.setDisable(false);
        lblUpdaterStatus.setText("работает");
        //control buttons
        btnUpdaterStart.setDisable(true);
        btnUpdaterStop.setDisable(false);
        //process elements
        lblUpdCurrentProd.setVisible(true);
        lblProgress.setVisible(true);
        prgrUpdater.setVisible(true);
        //manual update
        fieldManualUpdate.setDisable(true);
        chkUpdSwitchSource.setDisable(true);
        btnUpdManualUpdate.setDisable(true);
    }

    void updaterStopped() {
        isUpdaterRun = false;
        //updater status
        lblUpdaterStatus.setDisable(true);
        lblUpdaterStatus.setText("выключен");
        //control buttons
        btnUpdaterStart.setDisable(false);
        btnUpdaterStop.setDisable(true);
        chkLastSavedPos.setDisable(false);
        //process elements
        lblUpdCurrentProd.setVisible(false);
        lblProgress.setVisible(false);
        prgrUpdater.setVisible(false);
        //manual update
        fieldManualUpdate.setDisable(false);
        chkUpdSwitchSource.setDisable(false);
        btnUpdManualUpdate.setDisable(false);

    }

    //researcher
    void setResearcherLastUpdate(String date) {
        lblResearcherLastRunText.setVisible(true);
        lblResearcherLastRunDate.setText(date);
        lblResearcherLastRunDate.setVisible(true);
    }

    void researcherStart() {
        showResearcherElements();
    }

    void researcherStopped() {
        isResearcherRun = false;
        //updater status
        lblResearcherStatus.setDisable(true);
        lblResearcherStatus.setText("выключен");
        //control buttons
        btnResearcherStart.setDisable(false);
        btnResearcherStop.setDisable(true);
        //process elements
        lblResearcherCategory.setVisible(true);
        lblResearcherCategoryPos.setVisible(false);
        lblResearcherCategoryName.setVisible(false);
        lblResearcherCurrentGroupName.setVisible(false);
        lblResearcherGroupPos.setVisible(false);
        prgrResearcher.setVisible(false);
        lblResearcherGroupPos.setVisible(false);
    }

    private void showResearcherElements() {
        //updater status
        lblResearcherStatus.setDisable(false);
        lblResearcherStatus.setText("работает");
        //control buttons
        btnResearcherStart.setDisable(true);
        btnResearcherStop.setDisable(false);
        //process elements
        lblResearcherCategory.setVisible(true);
        lblResearcherCategoryPos.setVisible(true);
        lblResearcherCategoryName.setVisible(true);
        lblResearcherCurrentGroupName.setVisible(true);
        prgrResearcher.setVisible(true);
        lblResearcherGroupPos.setVisible(true);
    }

    void setResearcherProgress(double progress, String position) {
        if (!isResearcherRun) {
            isResearcherRun = true;
            showResearcherElements();
        }
        prgrResearcher.setProgress(progress);
        lblResearcherGroupPos.setText(position);
    }

    void setResearcherCurrentGroup(String groupName) {
        lblResearcherCurrentGroupName.setText(groupName);
    }

    void setResearcherCurrentCategory(String position, String categoryName) {
        lblResearcherCategoryPos.setText(position);
        lblResearcherCategoryName.setText(categoryName);
    }

    void setResearcherTotalFounds(String count) {
        //TODO researcher total products
    }

    void appendResearcherFoundProd(String diff) {
        if (researcherLogArea.isDisable()) researcherLogArea.setDisable(false);
        researcherLogArea.appendText(diff + "\n");
    }

    private void resetUI() {
        researcherStopped();
        updaterStopped();
        clearProductList();
    }

    void setResearchFailed(String count) {
        Platform.runLater(() -> {
//            if (!isUpdaterRun) {
//                isUpdaterRun = true;
//                showUpdaterElements();
//            }
//            if (!lblUpdateFailed.isVisible()) {
//                lblUpdateFailed.setVisible(true);
//                lblUpdateFailsCount.setVisible(true);
//            }
//            lblUpdateFailsCount.setText(count);
            /*TODO*/
        });
    }

    //products
    void addCityToComb(String city) {
        combCity.getItems().add(city);
    }

    void resetProductComboBoxes() {
        chkStock.setSelected(false);
        combStore.setDisable(true);
        combCity.getSelectionModel().select(0);
        combStore.getSelectionModel().select(0);
    }

    void updateProductComboBoxes(boolean stock, String city, String store) {
        chkStock.setSelected(stock);
        combStore.setDisable(!stock);
        if (city == null) {
            combCity.getSelectionModel().select(0);
            combStore.getSelectionModel().select(0);
            return;
        } else if (!combCity.getSelectionModel().getSelectedItem().equals(city)) {
            combCity.getSelectionModel().select(city);
            List<String> list = listener.storeListRequest(city);
            if (list != null) {
                combStore.getItems().setAll(list);
            }
        }
        combStore.getSelectionModel().select(store);
    }

    void updateProductTableContent(List<Product> products) {
        if (productsList == null) {
            productsList = FXCollections.observableArrayList(products);
        } else {
            productsList.removeAll(productsList);
            productsList.addAll(products);
        }

        totalProductShow.setText("Всего найдено: " + products.size());

        productTableView.setItems(productsList);
//            productTableView.refresh();
    }

    void addToProductList(Product product) {
        if (productsList == null) {
            productsList = FXCollections.observableArrayList(product);
        } else {
            productsList.add(product);
        }

        productTableView.setItems(productsList);
        //productTableView.refresh();
    }

    void clearProductList() {
        if (productsList == null) return;
        productsList.clear();
        productTableView.getItems().clear();
    }

    void productsNotFound() {
        makeDialogWindow(Alert.AlertType.INFORMATION, "Нет данных",
                "По вашему запросу не было найдено соответствий продуктам",
                "Измените ваш запрос (уменьшите количество условий, измените его содержание").showAndWait();
    }

    //common
    private Alert makeDialogWindow(Alert.AlertType type, String title, String header, String context) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(context);
        return alert;
    }

    void failedToKickUser(String nickname) {
        makeDialogWindow(Alert.AlertType.ERROR, "Ошибка!", "Невозможно отключить пользователя: " + nickname, "Недостаточный уровень прав!").showAndWait();
    }

    void kickedFromTheServer(String nickname) {
        makeDialogWindow(Alert.AlertType.ERROR, "Соединение прервано!", "Вы были отключены от сервера пользователем: " + nickname, "").showAndWait();
    }

    void connectionLost() {
        resetUI();
    }

    //Event Handlers
    @FXML
    private void handleUpdaterStartButton() {
        btnUpdaterStart.setDisable(true);
        chkLastSavedPos.setDisable(true);
        listener.startUpdaterRequest(chkLastSavedPos.isSelected());
    }

    @FXML
    private void handleUpdaterStopButton() {
        btnUpdaterStop.setDisable(true);
        listener.stopUpdaterRequest();
    }

    @FXML
    private void handleResearcherStartButton() {
        btnResearcherStart.setDisable(true);
        listener.startResearcherRequest();
    }

    @FXML
    private void handleResearcherStopButton() {
        btnResearcherStop.setDisable(true);
        listener.stopResearcherRequest();
    }

    @FXML
    private void handleUpdaterSwitchSourceCheckbox() {
        fieldManualUpdate.setText("");
        if (chkUpdSwitchSource.isSelected()) {
            fieldManualUpdate.setPromptText("URL");
        } else {
            fieldManualUpdate.setPromptText("id");
        }
    }

    @FXML
    private void handleManualUpdateButton() {
        /*TODO make manual update*/
    }

    @FXML
    private void handleBanUserButton() {
        Optional<ButtonType> result = makeDialogWindow(Alert.AlertType.CONFIRMATION, "", "", "").showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            /*TODO handle ban user action*/
        }
    }

    @FXML
    private void handleKickUserButton() {
        String nickname = updaterUsersListView.getSelectionModel().getSelectedItem().getText();
        Optional<ButtonType> result = makeDialogWindow(Alert.AlertType.CONFIRMATION, "Отключение пользователя", "Вы действительно хотите отключить пользователя " + nickname + "?", "").showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            listener.kickUserRequest(nickname);
        }
        btnKickUser.setDisable(true);
        btnBanUser.setDisable(true);
    }

    @FXML
    private void handleListViewAction(MouseEvent event) {
        ListView listView = (ListView) event.getSource();
        Object obj = listView.getSelectionModel().getSelectedItem();
        if (obj != null) {
            btnKickUser.setDisable(false);
            btnBanUser.setDisable(false);
        } else {
            btnKickUser.setDisable(true);
            btnBanUser.setDisable(true);
        }
    }

    @FXML
    private void handleCityCombEvent() {
        String selectedItem = combCity.getSelectionModel().getSelectedItem();
        if (selectedItem == null || selectedItem.equals("Все города")) {
            combStore.setDisable(true);
        } else {
            combStore.setDisable(false);
            combStore.getItems().clear();
            combStore.getItems().addAll(listener.storeListRequest(selectedItem));

            /*TODO разобраться с этиим костылём ниже (есть ли какие-то альтернативы для корректного ресайза выпадающего списка?)*/
            combStore.show();
            combStore.hide();
        }
        combStore.getSelectionModel().select(0);
    }

    @FXML
    private void handleShowProductFilter(ActionEvent event) {
        app.showProductFilter(chkStock.isSelected(), combCity.getItems(), combCity.getSelectionModel().getSelectedIndex(), combStore.getItems(), combStore.getSelectionModel().getSelectedIndex());
    }

    @FXML
    private void handleBtnSettingsEvent() {
        app.showSettingsStage();
    }

    void setApp(AppGUI appGUI) {
        this.app = appGUI;
    }
}
