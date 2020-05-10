import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;

import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

public class GUIController implements GUIEvents {
    private Controller controller;
    private int updaterTotal;
    private int updaterErrors;
    private double updaterProgressPoint;

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private Button btnServerStart;
    @FXML
    private Button btnServerStop;

    @FXML
    private Button btnUpdaterStart;
    @FXML
    private Button btnUpdaterStop;

    @FXML
    private Label lblServerStatus;
    @FXML
    private Label lblProductsCount;
    @FXML
    private Label lblWarehousesCount;
    @FXML
    private Label lblLastUpdatedMain;

    @FXML
    private Label lblUpdaterStatus;
    @FXML
    private Label lblUpdaterLastUpdated;
    @FXML
    private Label lblUpdaterTotalChecked;
    @FXML
    private Label lblUpdaterUpdatedCount;
    @FXML
    private Label lblUpdateFailed;
    @FXML
    private Label lblUpdaterCurrentProgress;
    @FXML
    private ProgressBar prgUpdater;


    @FXML
    private TextArea txtAreaLogger;

    @FXML
    void initialize() {
        controller = new Controller(this);
    }


    //Buttons Handlers
    @FXML
    private void handleServerStartButton() {

    }

    @FXML
    private void handleServerStopButton() {

    }

    //Updater
    @FXML
    private void handleUpdaterStartButton() {
        controller.startUpdater();
        txtAreaLogger.appendText("Updater started!\n");
    }

    @FXML
    private void handleUpdaterStopButton() {
        System.out.println("stop");
        controller.stopUpdater();
    }


    //Controller Events
    @Override
    public void onUpdaterStart() {
        Platform.runLater(() -> {
            btnUpdaterStart.setDisable(true);
            btnUpdaterStop.setDisable(false);
            prgUpdater.setProgress(0.0d);
            lblUpdaterStatus.setText("Online");
            lblUpdaterStatus.setTextFill(Color.GREEN);
            prgUpdater.setVisible(true);
            lblUpdaterTotalChecked.setVisible(false);
            lblUpdaterUpdatedCount.setVisible(false);
            lblUpdateFailed.setVisible(false);

        });
    }

    @Override
    public void onUpdaterTotalProducts(int count) {
        updaterTotal = count;
        updaterProgressPoint = (double) 1 / count;
        Platform.runLater(() -> {
            lblUpdaterCurrentProgress.setText(0 + "/" + count);
            lblUpdaterCurrentProgress.setVisible(true);
        });
    }

    @Override
    public void onUpdaterProductFailed(String url) {
        updaterErrors++;
        lblUpdateFailed.setVisible(true);
        lblUpdateFailed.setText(String.valueOf(updaterErrors));
        txtAreaLogger.appendText("Failed(" + updaterErrors + ") to parse: !" + url + "\n");
    }

    @Override
    public void onUpdaterCurrentProduct(int position) {
        Platform.runLater(() -> {
            lblUpdaterCurrentProgress.setText(position + "/" + updaterTotal);
            prgUpdater.setProgress(updaterProgressPoint * position);
            lblUpdaterTotalChecked.setVisible(true);
            lblUpdaterTotalChecked.setText(String.valueOf(position));
        });
    }

    @Override
    public void onUpdaterEnd(int checked, int updated) {
        Platform.runLater(() -> {
            btnUpdaterStart.setDisable(false);
            btnUpdaterStop.setDisable(true);
            lblUpdaterStatus.setText("Offline");
            lblUpdaterStatus.setTextFill(Color.GRAY);
            lblUpdaterLastUpdated.setText(DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault()).format(new Date(System.currentTimeMillis())));
            lblUpdaterTotalChecked.setVisible(true);
            lblUpdaterUpdatedCount.setVisible(true);
            lblUpdateFailed.setVisible(true);
            lblUpdaterTotalChecked.setText(String.valueOf(checked));
            lblUpdaterUpdatedCount.setText(String.valueOf(updated));
            lblUpdateFailed.setText(String.valueOf(updaterErrors));
            prgUpdater.setVisible(false);
            lblUpdaterCurrentProgress.setVisible(false);
            txtAreaLogger.appendText("Updater successful end. Checked: " + checked +", updated: " + updated + "\n");
        });
    }
}
