import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class ClientGUIController {
    private Client client;
    private boolean isUpdaterRun;
    private boolean isResearcherRun;
    private String prevProdSelectedItem;

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
    private Label lblResearchCategory;
    @FXML
    private Label lblResearchCategoryPos;
    @FXML
    private Label lblResearchCategoryName;
    @FXML
    private Label lblResearchCurrentGroupName;
    @FXML
    private Label lblResearchGroupPos;
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
    void initialize(){
        combCity.getItems().add("Все города");
        combCity.getSelectionModel().select(0);
        combStore.getItems().add("Все магазины");
        combStore.getSelectionModel().select(0);
    }

    void setClient(Client client) {
        this.client = client;
    }

    void setNickname(String nickname) {
        Platform.runLater(() -> {
            lblNickname.setText(nickname);
        });
    }

    void setTabsEnableForModerator() {
        Platform.runLater(() -> {
            tabUpdater.setDisable(false);
            tabResearcher.setDisable(false);
            tabProducts.setDisable(false);
        });
    }

    void setTabsEnableForAdmin() {
        Platform.runLater(() -> {
            tabUpdater.setDisable(false);
            tabResearcher.setDisable(false);
            tabProducts.setDisable(false);
            tabUsers.setDisable(false);
            tabGrabber.setDisable(false);
        });
    }

    void setServerUpTime(String time) {
        Platform.runLater(() -> {
            lblServerUpTime.setText(time);
        });
    }

    void setProductsCount(String count) {
        Platform.runLater(() -> {
            lblProductsCount.setText(count);
        });
    }

    void setWarehousesCount(String count) {
        Platform.runLater(() -> {
            lblWarehousesCount.setText(count);
        });
    }

    void setActiveUsersCount(String count) {
        Platform.runLater(() -> {
            lblActiveUsers.setText(count);
        });
    }

    void updateUsersList(String[] users) {
        Platform.runLater(() -> {
            updaterUsersListView.setDisable(users.length == 1);
            updaterUsersListView.getItems().clear();
            String ownNick = lblNickname.getText();
            for (String nickname : users) {
//                if(ownNick.equals(nickname)) nickname = nickname + " (вы)";
                if (ownNick.equals(nickname)) continue;
                updaterUsersListView.getItems().add(new Label(nickname));
            }
        });
    }

    //Updater
    void updaterStart() {
        Platform.runLater(this::showUpdaterElements);
    }

    void setUpdaterProgress(double progress, String progressText) {
        Platform.runLater(() -> {
            if (!isUpdaterRun) {
                isUpdaterRun = true;
                showUpdaterElements();
            }
            chkLastSavedPos.setDisable(true);
            prgrUpdater.setProgress(progress);
            lblProgress.setText(progressText);
        });
    }

    void setLastPositionCheckboxVisible(boolean state) {
        Platform.runLater(() -> {
            chkLastSavedPos.setVisible(state);
            chkLastSavedPos.setDisable(isUpdaterRun);
        });
    }

    void setUpdaterCurrentProduct(String productName) {
        Platform.runLater(() -> {
            if (!isUpdaterRun) {
                isUpdaterRun = true;
                showUpdaterElements();
            }
            lblUpdCurrentProd.setText(productName);
        });
    }

    void setUpdatesFound(String count) {
        Platform.runLater(() -> {
            if (!isUpdaterRun) {
                isUpdaterRun = true;
                showUpdaterElements();
            }
            if (!lblUpdated.isVisible()) {
                lblUpdated.setVisible(true);
                lblUpdatesCount.setVisible(true);
            }
            lblUpdatesCount.setText(count);
        });
    }

    void setUpdatesFailed(String count) {
        Platform.runLater(() -> {
            if (!isUpdaterRun) {
                isUpdaterRun = true;
                showUpdaterElements();
            }
            if (!lblUpdateFailed.isVisible()) {
                lblUpdateFailed.setVisible(true);
                lblUpdateFailsCount.setVisible(true);
            }
            lblUpdateFailsCount.setText(count);
        });
    }

    void appendDifferencesFound(String diff) {
        Platform.runLater(() -> {
            if (updaterLogArea.isDisable()) updaterLogArea.setDisable(false);
            updaterLogArea.appendText(diff + "\n");
        });
    }

    void appendErrorToUpdaterLogger(String reason, String exceptionMsg) {
        Platform.runLater(() -> {
            if (updaterLogArea.isDisable()) updaterLogArea.setDisable(false);
            updaterLogArea.appendText("Error: " + reason + ": " + exceptionMsg + "\n");
        });
    }

    void appendErrorToUpdaterLogger(String id, String url, String exceptionMsg) {
        Platform.runLater(() -> {
            if (updaterLogArea.isDisable()) updaterLogArea.setDisable(false);
            updaterLogArea.appendText("Error of product with id: " + id + ", url: " + url + ", " + exceptionMsg + "\n");
        });
    }

    void setUpdaterLastRun(String date) {
        Platform.runLater(() -> {
            lblUpdaterLastRunText.setVisible(true);
            lblUpdaterLastRunDate.setText(date);
            lblUpdaterLastRunDate.setVisible(true);
        });
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

    void updaterStop() {
        isUpdaterRun = false;
        Platform.runLater(() -> {
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
        });
    }

//    void connectionLost() {
//        Platform.runLater(() -> {
//            showDialog(Alert.AlertType.ERROR, "Ошибка!", "Потеряно соединение с сервером", "").showAndWait();
//        });
//    }

    //researcher
    void setResearcherLastUpdate(String date) {
        Platform.runLater(() -> {
            lblResearcherLastRunText.setVisible(true);
            lblResearcherLastRunDate.setText(date);
            lblResearcherLastRunDate.setVisible(true);
        });
    }

    void researcherStart() {
        Platform.runLater(this::showResearcherElements);
    }

    void researcherEnd() {
        isResearcherRun = false;
        Platform.runLater(() -> {
            //updater status
            lblResearcherStatus.setDisable(true);
            lblResearcherStatus.setText("выключен");
            //control buttons
            btnResearcherStart.setDisable(false);
            btnResearcherStop.setDisable(true);
            //process elements
            lblResearchCategory.setVisible(true);
            lblResearchCategoryPos.setVisible(false);
            lblResearchCategoryName.setVisible(false);
            lblResearchCurrentGroupName.setVisible(false);
            prgrResearcher.setVisible(false);
            lblResearchGroupPos.setVisible(false);
        });
    }

    private void showResearcherElements() {
        //updater status
        lblResearcherStatus.setDisable(false);
        lblResearcherStatus.setText("работает");
        //control buttons
        btnResearcherStart.setDisable(true);
        btnResearcherStop.setDisable(false);
        //process elements
        lblResearchCategory.setVisible(true);
        lblResearchCategoryPos.setVisible(true);
        lblResearchCategoryName.setVisible(true);
        lblResearchCurrentGroupName.setVisible(true);
        prgrResearcher.setVisible(true);
        lblResearchGroupPos.setVisible(true);
    }

    void setResearcherProgress(double progress, String position) {
        Platform.runLater(() -> {
            if (!isResearcherRun) {
                isResearcherRun = true;
                showResearcherElements();
            }
            prgrResearcher.setProgress(progress);
            lblResearchGroupPos.setText(position);
        });
    }

    void setResearcherCurrentGroup(String groupName) {
        Platform.runLater(() -> {
            lblResearchCurrentGroupName.setText(groupName);
        });
    }

    void setResearcherCurrentCategory(String position, String categoryName) {
        Platform.runLater(() -> {
            lblResearchCategoryPos.setText(position);
            lblResearchCategoryName.setText(categoryName);
        });
    }

    void setResearcherTotalFounds(String count){
        /*TODO*/
    }

    void appendResearcherFoundProd(String diff){
        Platform.runLater(() -> {
            if (researcherLogArea.isDisable()) researcherLogArea.setDisable(false);
            researcherLogArea.appendText(diff + "\n");
        });
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
    void addCityToComb(String city){
        Platform.runLater(() -> {
            combCity.getItems().add(city);
        });
    }

    void resetProductComboBoxes(){
        Platform.runLater(() -> {
            chkStock.setSelected(false);
            combStore.setDisable(true);
            combCity.getSelectionModel().select(0);
            combStore.getSelectionModel().select(0);
        });
    }

    void updateProductComboBoxes(boolean stock, String city, String store) {
        Platform.runLater(() -> {
            chkStock.setSelected(stock);
            combStore.setDisable(!stock);
            if (city == null){
                combCity.getSelectionModel().select(0);
                combStore.getSelectionModel().select(0);
                return;
            }
            else if (!combCity.getSelectionModel().getSelectedItem().equals(city)){
                combCity.getSelectionModel().select(city);
                List<String> list = client.getStoreList(city);
                if (list != null){
                    combStore.getItems().setAll(list);
                }
            }
            combStore.getSelectionModel().select(store);
        });
    }

    //common
    private Alert showDialog(Alert.AlertType type, String title, String header, String context) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(context);
        return alert;
    }

    void failedToKickUser(String nickname) {
        Platform.runLater(() -> {
            showDialog(Alert.AlertType.ERROR, "Ошибка!", "Невозможно отключить пользователя: " + nickname, "Недостаточный уровень прав!").showAndWait();
        });
    }

    void kickedFromTheServer(String nickname) {
        Platform.runLater(() -> {
            showDialog(Alert.AlertType.ERROR, "Соединение прервано!", "Вы были отключены от сервера пользователем: " + nickname, "").showAndWait();
        });
    }



    //Event Handlers
    @FXML
    void handleUpdaterStartButton() {
        btnUpdaterStart.setDisable(true);
        chkLastSavedPos.setDisable(true);
        client.startUpdater(chkLastSavedPos.isSelected());
    }

    @FXML
    void handleUpdaterStopButton() {
        btnUpdaterStop.setDisable(true);
        client.stopUpdater();
    }

    @FXML
    void handleResearcherStartButton() {
        btnResearcherStart.setDisable(true);
        client.startResearcher();
    }

    @FXML
    void handleResearcherStopButton() {
        btnResearcherStop.setDisable(true);
        client.stopResearcher();
    }

    @FXML
    void handleUpdaterSwitchSourceCheckbox() {
        fieldManualUpdate.setText("");
        if (chkUpdSwitchSource.isSelected()) {
            fieldManualUpdate.setPromptText("URL");
        } else {
            fieldManualUpdate.setPromptText("id");
        }
    }

    @FXML
    void handleManualUpdateButton() {
        /*TODO make manual update*/
    }

    @FXML
    void handleBanUserButton() {
        Optional<ButtonType> result = showDialog(Alert.AlertType.CONFIRMATION, "", "", "").showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            /*TODO handle ban user action*/
        }
    }

    @FXML
    void handleKickUserButton() {
        String nickname = updaterUsersListView.getSelectionModel().getSelectedItem().getText();
        Optional<ButtonType> result = showDialog(Alert.AlertType.CONFIRMATION, "Отключение пользователя", "Вы действительно хотите отключить пользователя " + nickname + "?", "").showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            client.kickUser(nickname);
        }
        btnKickUser.setDisable(true);
        btnBanUser.setDisable(true);
    }

    @FXML
    void handleListViewAction(MouseEvent event) {
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
    void handleCityCombEvent(ActionEvent event) {
        String selectedItem = combCity.getSelectionModel().getSelectedItem();
        if (selectedItem == null || selectedItem.equals("Все города")){
            combStore.setDisable(true);
        } else {
            combStore.setDisable(false);
            combStore.getItems().clear();
            combStore.getItems().addAll(client.getStoreList(selectedItem));

            /*TODO разобраться с этиим костылём ниже (есть ли какие-то альтернативы для корректного ресайза выпадающего списка?)*/
            combStore.show();
            combStore.hide();
            //            combStore.autosize();
        }
        combStore.getSelectionModel().select(0);
    }

    @FXML
    void handleShowProductFilter(ActionEvent event) {
            client.showProductFilterStage(chkStock.isSelected(), combCity.getItems(), combCity.getSelectionModel().getSelectedIndex(), combStore.getItems(), combStore.getSelectionModel().getSelectedIndex());

//        System.out.println(combCity.getSelectionModel().getSelectedItem());
//        System.out.println(combStore.getSelectionModel().getSelectedItem());
        //client.showProductFilter();
    }

}
