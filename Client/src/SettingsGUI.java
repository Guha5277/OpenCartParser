import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Window;

import java.time.LocalTime;

public class SettingsGUI {
    private Controller controller;
    private boolean updaterAutostartState;
    private boolean researcherAutostartState;
    private LocalTime updaterAutostartTime;
    private LocalTime researcherAutostartTime;
    private int updaterDaysInterval;
    private int researcherDaysInterval;
    private boolean stateInitialize;

    @FXML
    private Window window;
    @FXML
    private CheckBox chkUpdaterEnable;
    @FXML
    private CheckBox chkResearcherEnable;
    @FXML
    private TextField txtUpdaterDay;
    @FXML
    private TextField txtUpdaterHour;
    @FXML
    private TextField txtUpdaterMinute;
    @FXML
    private TextField txtResearcherDay;
    @FXML
    private TextField txtResearcherHour;
    @FXML
    private TextField txtResearcherMinute;
    @FXML
    private Button btnOk;

    public void setController(Controller controller) {
        this.controller = controller;
    }

    void setUpdaterAutostartState(boolean updaterAutostartState) {
        this.updaterAutostartState = updaterAutostartState;
    }

    void setResearcherAutostartState(boolean researcherAutostartState) {
        this.researcherAutostartState = researcherAutostartState;
    }

    void setUpdaterAutostartTime(LocalTime updaterAutostartTime) {
        this.updaterAutostartTime = updaterAutostartTime;
    }

    void setResearcherAutostartTime(LocalTime researcherAutostartTime) {
        this.researcherAutostartTime = researcherAutostartTime;
    }

    void setUpdaterDaysInterval(int updaterDaysInterval) {
        this.updaterDaysInterval = updaterDaysInterval;
    }

    void setResearcherDaysInterval(int researcherDaysInterval) {
        this.researcherDaysInterval = researcherDaysInterval;
    }

    @FXML
    void initialize() {
        txtUpdaterDay.textProperty().addListener(new FieldListener(txtUpdaterDay, 100));
        txtUpdaterHour.textProperty().addListener(new FieldListener(txtUpdaterHour, 23));
        txtUpdaterMinute.textProperty().addListener(new FieldListener(txtUpdaterMinute, 59));
        txtResearcherDay.textProperty().addListener(new FieldListener(txtResearcherDay, 100));
        txtResearcherHour.textProperty().addListener(new FieldListener(txtResearcherHour, 23));
        txtResearcherMinute.textProperty().addListener(new FieldListener(txtResearcherMinute, 59));

        txtUpdaterDay.focusedProperty().addListener(new FocusChangeListener(txtUpdaterDay, "1"));
        txtUpdaterHour.focusedProperty().addListener(new FocusChangeListener(txtUpdaterHour, "00"));
        txtUpdaterMinute.focusedProperty().addListener(new FocusChangeListener(txtUpdaterMinute, "00"));
        txtResearcherDay.focusedProperty().addListener(new FocusChangeListener(txtResearcherDay, "1"));
        txtResearcherHour.focusedProperty().addListener(new FocusChangeListener(txtResearcherHour, "00"));
        txtResearcherMinute.focusedProperty().addListener(new FocusChangeListener(txtResearcherMinute, "00"));


        Platform.runLater(() -> {
            window = chkResearcherEnable.getScene().getWindow();
            window.setOnShown(event -> {
                chkUpdaterEnable.setSelected(updaterAutostartState);
                chkResearcherEnable.setSelected(researcherAutostartState);

                txtUpdaterDay.setText(String.valueOf(updaterDaysInterval));
                txtUpdaterHour.setText(String.valueOf(updaterAutostartTime.getHour()));
                txtUpdaterMinute.setText(String.valueOf(updaterAutostartTime.getMinute()));

                txtResearcherDay.setText(String.valueOf(researcherDaysInterval));
                txtResearcherHour.setText(String.valueOf(researcherAutostartTime.getHour()));
                txtResearcherMinute.setText(String.valueOf(researcherAutostartTime.getMinute()));

                txtUpdaterDay.setDisable(!updaterAutostartState);
                txtUpdaterHour.setDisable(!updaterAutostartState);
                txtUpdaterMinute.setDisable(!updaterAutostartState);
                txtResearcherDay.setDisable(!researcherAutostartState);
                txtResearcherHour.setDisable(!researcherAutostartState);
                txtResearcherMinute.setDisable(!researcherAutostartState);
                stateInitialize = true;
            });
        });
    }

    private void checkStatement() {
        if (!stateInitialize) return;
        btnOk.setDisable(!(Integer.parseInt(txtUpdaterDay.getText()) != updaterDaysInterval ||
                Integer.valueOf(txtUpdaterHour.getText()) != updaterAutostartTime.getHour() ||
                Integer.valueOf(txtUpdaterMinute.getText()) != updaterAutostartTime.getMinute() ||
                Integer.parseInt(txtResearcherDay.getText()) != researcherDaysInterval ||
                Integer.valueOf(txtResearcherHour.getText()) != researcherAutostartTime.getHour() ||
                Integer.valueOf(txtResearcherMinute.getText()) != researcherAutostartTime.getMinute()
                ));
    }

    @FXML
    private void handleCheckBoxUpdaterEvent() {
        boolean isSelected = chkUpdaterEnable.isSelected();
        txtUpdaterDay.setDisable(!isSelected);
        txtUpdaterHour.setDisable(!isSelected);
        txtUpdaterMinute.setDisable(!isSelected);
    }

    @FXML
    private void handleCheckBoxResearcherEvent() {
        boolean isSelected = chkResearcherEnable.isSelected();
        txtResearcherDay.setDisable(!isSelected);
        txtResearcherHour.setDisable(!isSelected);
        txtResearcherMinute.setDisable(!isSelected);
    }

    @FXML
    private void handleCancelButtonEvent(){
        window.hide();
    }

    @FXML
    private void handleOkButton(){
        boolean updaterEnable = chkUpdaterEnable.isSelected();
        boolean researcherEnable = chkResearcherEnable.isSelected();
        int updaterInterval = Integer.parseInt(txtUpdaterDay.getText());
        int researcherInterval = Integer.parseInt(txtResearcherDay.getText());
        LocalTime updaterTime = LocalTime.of(Integer.parseInt(txtUpdaterHour.getText()), Integer.parseInt(txtResearcherMinute.getText()));
        LocalTime researcherTime = LocalTime.of(Integer.parseInt(txtResearcherHour.getText()), Integer.parseInt(txtResearcherMinute.getText()));
        controller.applySettings(updaterEnable, researcherEnable, updaterInterval, researcherInterval, updaterTime, researcherTime);
        window.hide();

        updaterAutostartState = updaterEnable;
        researcherAutostartState = researcherEnable;
        updaterAutostartTime = updaterTime;
        researcherAutostartTime = researcherTime;
    }

    private class FieldListener implements ChangeListener<String> {
        TextField field;
        int maxValue;
        String maxValueString;

        FieldListener(TextField field, int maxValue) {
            this.field = field;
            this.maxValue = maxValue;
            maxValueString = String.valueOf(maxValue);
        }

        @Override
        public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
            if (!newValue.matches("\\d*")) {
                field.setText(newValue.replaceAll("[^\\d]", ""));
            }
            checkValue();
            if (!field.getText().equals(""))checkStatement();
        }

        private void checkValue() {
            if (field.getText().equals("")) return;
            Integer currentValue = Integer.valueOf(field.getText());
            if (currentValue > maxValue) field.setText(maxValueString);
        }
    }

    private class FocusChangeListener implements ChangeListener<Boolean> {
        TextField field;
        String minValue;

        FocusChangeListener(TextField field, String minValue) {
            this.field = field;
            this.minValue = minValue;
        }

        @Override
        public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
            if (!newValue && field.getText().equals("")) {
                field.setText(minValue);
                checkStatement();
            }
        }
    }

}
