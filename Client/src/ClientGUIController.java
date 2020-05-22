import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;


public class ClientGUIController {
    private Client client;
    private boolean isUpdaterRun;
    @FXML
    private ResourceBundle resources;
    @FXML
    private URL location;
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

    //Updater Part
    @FXML
    private Label lblUpdaterStatus;
    @FXML
    private Button btnUpdaterStart;
    @FXML
    private Button btnUpdaterStop;
    @FXML
    private Label lblLastRun;
    @FXML
    private Label lblUpdated;
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

    //Updater
    void setUpdaterProgress(double progress, String progressText) {
        Platform.runLater(() -> {
            if (!isUpdaterRun) {
                isUpdaterRun = true;
                showUpdaterElements();
            }
            prgrUpdater.setProgress(progress);
            lblProgress.setText(progressText);
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
            if (!lblUpdated.isVisible()){
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
            if (!lblUpdateFailed.isVisible()){
                lblUpdateFailed.setVisible(true);
                lblUpdateFailsCount.setVisible(true);
            }
            lblUpdateFailsCount.setText(count);
        });
    }

    void updaterStart() {
        Platform.runLater(this::showUpdaterElements);
    }

    private void showUpdaterElements() {
        btnUpdaterStart.setDisable(true);
        btnUpdaterStop.setDisable(false);
//        lblUpdated.setVisible(true);
//        lblUpdateFailed.setVisible(true);
//        lblUpdatesCount.setVisible(true);
//        lblUpdateFailsCount.setVisible(true);
        lblUpdCurrentProd.setVisible(true);
        lblProgress.setVisible(true);
        prgrUpdater.setVisible(true);
    }

    void updaterStop() {
        Platform.runLater(() -> {
            isUpdaterRun = false;
            btnUpdaterStart.setDisable(false);
            btnUpdaterStop.setDisable(true);
//            lblUpdated.setVisible(true);
//            lblUpdateFailed.setVisible(true);
//            lblUpdatesCount.setVisible(true);
//            lblUpdateFailsCount.setVisible(true);
            lblUpdCurrentProd.setVisible(false);
            prgrUpdater.setVisible(false);
            lblProgress.setVisible(false);
        });
    }

    //Event Handlers
    @FXML
    void handleUpdaterStartButton() {
        btnUpdaterStart.setDisable(true);
        client.startUpdater();
    }

    @FXML
    void handleUpdaterStopButton() {
        btnUpdaterStop.setDisable(true);
        client.stopUpdater();
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
}
