import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public class ProductFilterGuiController {
    Client client;
    @FXML
    private ComboBox<String> combCity;
    @FXML
    private CheckBox chkStock;
    @FXML
    private ComboBox<String> combStore;
    @FXML
    private TextField fieldStrengthStart;
    @FXML
    private TextField fieldStrengthEnd;
    @FXML
    private CheckBox chkNicSalt;
    @FXML
    private CheckBox chkNicClassic;
    @FXML
    private TextField fieldVolumeStart;
    @FXML
    private TextField fieldVolumeEnd;
    @FXML
    private TextField fieldPriceStart;
    @FXML
    private TextField fieldPriceEnd;
    @FXML
    private ComboBox<String> combTaste;
    @FXML
    private Button btnTaste;
    @FXML
    private Button btnOk;
    @FXML
    private Button btnDiscard;
    @FXML
    private Button btnCancel;

    private boolean stateInitialize;
    private boolean wrongProductFilterRange;
    private int wrongProductFilterRangeCount;
    private boolean stock;
    private boolean nicSalt;
    private boolean nicClassic;
    private String city;
    private String store;
    private String strengthStart;
    private String strengthEnd;
    private String volumeStart;
    private String volumeEnd;
    private String priceStart;
    private String priceEnd;

    private String defaultFieldStyle;

    @FXML
    void initialize() {
        fieldStrengthStart.textProperty().addListener(new FieldListener(fieldStrengthStart));
        fieldStrengthEnd.textProperty().addListener(new FieldListener(fieldStrengthEnd));
        fieldVolumeStart.textProperty().addListener(new FieldListener(fieldVolumeStart));
        fieldVolumeEnd.textProperty().addListener(new FieldListener(fieldVolumeEnd));
        fieldPriceStart.textProperty().addListener(new FieldListener(fieldPriceStart));
        fieldPriceEnd.textProperty().addListener(new FieldListener(fieldPriceEnd));
        defaultFieldStyle = fieldStrengthStart.getStyle();
    }

    private void updateStatement() {
        stock = chkStock.isSelected();
        nicSalt = chkNicSalt.isSelected();
        nicClassic = chkNicClassic.isSelected();
        city = combCity.getSelectionModel().getSelectedItem();
        store = combStore.getSelectionModel().getSelectedItem();
        strengthStart = fieldStrengthStart.getText();
        strengthEnd = fieldStrengthEnd.getText();
        volumeStart = fieldVolumeStart.getText();
        volumeEnd = fieldVolumeEnd.getText();
        priceStart = fieldPriceStart.getText();
        priceEnd = fieldPriceEnd.getText();
        stateInitialize = true;
    }

    private void checkStatement() {
        if (!stateInitialize || wrongProductFilterRange) return;
        btnOk.setDisable(!(!stock == chkStock.isSelected() ||
                !city.equals(combCity.getSelectionModel().getSelectedItem()) ||
                !store.equals(combStore.getSelectionModel().getSelectedItem()) ||
                !strengthStart.equals(fieldStrengthStart.getText()) ||
                !strengthEnd.equals(fieldStrengthEnd.getText()) ||
                !volumeStart.equals(fieldVolumeStart.getText()) ||
                !volumeEnd.equals(fieldVolumeEnd.getText()) ||
                !priceStart.equals(fieldPriceStart.getText()) ||
                !priceEnd.equals(fieldPriceEnd.getText())
        ));
    }

    void setClient(Client client) {
        this.client = client;
    }

    void setStockParam(boolean inStockSelected, ObservableList<String> cityList, int selectedCity, ObservableList<String> storeList, int selectedStore) {
        combCity.setItems(cityList);
        combCity.getSelectionModel().select(selectedCity);
        combStore.setItems(storeList);
        combStore.getSelectionModel().select(selectedStore);
        combStore.setDisable(combCity.getSelectionModel().getSelectedItem().equals("Все города"));
        chkStock.setSelected(inStockSelected);
        combCity.setDisable(!inStockSelected);
        combStore.setDisable(!inStockSelected);
        updateStatement();
        checkStatement();
    }

    private void checkRanges() {
        String txtStrStart = fieldStrengthStart.getText();
        String txtStrEnd = fieldStrengthEnd.getText();
        String txtVolumeStart = fieldVolumeStart.getText();
        String txtVolumeEnd = fieldVolumeEnd.getText();
        String txtPriceStart = fieldPriceStart.getText();
        String txtPriceEnd = fieldPriceEnd.getText();

        int wrongRangesCount = 0;

        if (!txtStrStart.equals("") && !txtStrEnd.equals("")) {
            if (Integer.parseInt(txtStrStart) > Integer.parseInt(txtStrEnd)) {
                wrongRange(fieldStrengthStart, fieldStrengthEnd);
                wrongRangesCount++;
            } else {
                validRange(fieldStrengthStart, fieldStrengthEnd);
            }
        }
        if (!txtVolumeStart.equals("") && !txtVolumeEnd.equals("")) {
            if (Integer.parseInt(txtVolumeStart) > Integer.parseInt(txtVolumeEnd)) {
                wrongRange(fieldVolumeStart, fieldVolumeEnd);
                wrongRangesCount++;
            } else {
                validRange(fieldVolumeStart, fieldVolumeEnd);
            }
        }
        if (!txtPriceStart.equals("") && !txtPriceEnd.equals("")) {
            if (Integer.parseInt(txtPriceStart) > Integer.parseInt(txtPriceEnd)) {
                wrongRange(fieldPriceStart, fieldPriceEnd);
                wrongRangesCount++;
            } else {
                validRange(fieldPriceStart, fieldPriceEnd);
            }
        }
        wrongProductFilterRange = wrongRangesCount > 0;
        btnOk.setDisable(wrongProductFilterRange);
    }

    private void wrongRange(TextField field, TextField field2) {
        field.setStyle("-fx-text-fill: red; -fx-border-color: red;");
        field2.setStyle("-fx-text-fill: red; -fx-border-color: red;");
    }

    private void validRange(TextField field, TextField field2) {
        field.setStyle(defaultFieldStyle);
        field2.setStyle(defaultFieldStyle);
    }


    @FXML
    void handleCheckBoxStock(ActionEvent event) {
        boolean enable = chkStock.isSelected();
        combCity.setDisable(!enable);
        combStore.setDisable(combCity.getSelectionModel().getSelectedItem().equals("Все города") || !enable);
        if (enable != stock && !wrongProductFilterRange) {
            btnOk.setDisable(false);
        } else {
            checkStatement();
        }
    }

    @FXML
    void handleCityCombEvent(ActionEvent event) {
        String selectedItem = combCity.getSelectionModel().getSelectedItem();
        if (selectedItem == null || selectedItem.equals("Все города")) {
            combStore.setDisable(true);
        } else {
            combStore.setDisable(false);
            combStore.getItems().clear();
            combStore.getItems().addAll(client.getStoreList(selectedItem));

            if (stateInitialize && !city.equals(selectedItem) && !wrongProductFilterRange) {
                btnOk.setDisable(false);
            } else {
                checkStatement();
            }

            /*TODO разобраться с этиим костылём ниже (есть ли какие-то альтернативы для корректного ресайза выпадающего списка?)*/
            combStore.show();
            combStore.hide();
            //  combStore.autosize();

        }
        combStore.getSelectionModel().select(0);
    }

    @FXML
    void handleStoreCombEvent(ActionEvent event) {
        String selectedItem = combStore.getSelectionModel().getSelectedItem();
        if (stateInitialize && !store.equals(selectedItem) && !wrongProductFilterRange) {
            btnOk.setDisable(false);
        } else {
            checkStatement();
        }
    }

    @FXML
    void handleCheckBoxNicSalt(ActionEvent event) {
        boolean selected = chkNicSalt.isSelected();
        if (selected) chkNicClassic.setSelected(false);
        if (stateInitialize && !nicSalt == selected && !wrongProductFilterRange) {
            btnOk.setDisable(false);
        } else {
            checkStatement();
        }
    }

    @FXML
    void handleCheckBoxNicClassic(ActionEvent event) {
        boolean selected = chkNicClassic.isSelected();
        if (selected) chkNicSalt.setSelected(false);
        if (stateInitialize && !nicClassic == selected && !wrongProductFilterRange) {
            btnOk.setDisable(false);
        } else {
            checkStatement();
        }
    }

    @FXML
    void handleDiscardButton() {
        wrongProductFilterRange = false;
        chkStock.setSelected(false);
        combCity.getSelectionModel().select(0);
        combCity.setDisable(true);
        combStore.getSelectionModel().select(0);
        combStore.setDisable(true);
        combTaste.getSelectionModel().select(0);
        chkNicSalt.setSelected(false);
        chkNicClassic.setSelected(false);
        fieldStrengthStart.setText("");
        fieldStrengthEnd.setText("");
        fieldVolumeStart.setText("");
        fieldVolumeEnd.setText("");
        fieldPriceStart.setText("");
        fieldPriceEnd.setText("");
        fieldStrengthStart.setStyle(defaultFieldStyle);
        fieldStrengthEnd.setStyle(defaultFieldStyle);
        fieldVolumeStart.setStyle(defaultFieldStyle);
        fieldVolumeEnd.setStyle(defaultFieldStyle);
        fieldPriceStart.setStyle(defaultFieldStyle);
        fieldPriceEnd.setStyle(defaultFieldStyle);
        btnOk.setDisable(false);
        //checkStatement();
    }

    @FXML
    void handleOkButton() {
        boolean stockChecked = chkStock.isSelected();
        String cityName = null;
        String storeName = null;
        if (stockChecked) {
            cityName = combCity.getSelectionModel().getSelectedItem();
            storeName = combStore.getSelectionModel().getSelectedItem();
        }
        String txtStrengthStart = fieldStrengthStart.getText();
        String txtStrengthEnd = fieldStrengthEnd.getText();
        String txtVolumeStart = fieldVolumeStart.getText();
        String txtVolumeEnd = fieldVolumeEnd.getText();
        String txtPriceStart = fieldPriceStart.getText();
        String txtPriceEnd = fieldPriceEnd.getText();

        int strengthStart = txtStrengthStart.equals("") ? -1 : Integer.parseInt(txtStrengthStart);
        int strengthEnd = txtStrengthEnd.equals("") ? -1 : Integer.parseInt(txtStrengthEnd);
        int volumeStart = txtVolumeStart.equals("") ? -1 : Integer.parseInt(txtVolumeStart);
        int volumeEnd = txtVolumeEnd.equals("") ? -1 : Integer.parseInt(txtVolumeEnd);
        int priceStart = txtPriceStart.equals("") ? -1 : Integer.parseInt(txtPriceStart);
        int priceEnd = txtPriceEnd.equals("") ? -1 : Integer.parseInt(txtPriceEnd);

        /*TODO nicotine checkboxes*/
        client.applyProductFilter(stockChecked, cityName, storeName, strengthStart, strengthEnd, volumeStart, volumeEnd, priceStart, priceEnd);
        fieldStrengthEnd.getScene().getWindow().hide();
    }

    @FXML
    void handleCancelButton() {
        chkNicClassic.getScene().getWindow().hide();
    }

    class FieldListener implements ChangeListener<String> {
        TextField field;

        FieldListener(TextField field) {
            this.field = field;
        }

        @Override
        public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
            if (!newValue.matches("\\d*")) {
                field.setText(newValue.replaceAll("[^\\d]", ""));
            }
            checkRanges();
        }
    }
}
