import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Border;

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
        fieldStrengthStart.textProperty().addListener(new FieldListener(fieldStrengthStart, fieldStrengthEnd, true));
        fieldStrengthEnd.textProperty().addListener(new FieldListener(fieldStrengthEnd, fieldStrengthStart, false));
        fieldVolumeStart.textProperty().addListener(new FieldListener(fieldVolumeStart, fieldVolumeEnd, true));
        fieldVolumeEnd.textProperty().addListener(new FieldListener(fieldVolumeEnd, fieldVolumeStart, false));
        fieldPriceStart.textProperty().addListener(new FieldListener(fieldPriceStart, fieldPriceEnd, true));
        fieldPriceEnd.textProperty().addListener(new FieldListener(fieldPriceEnd, fieldPriceStart, false));
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
        if (!stateInitialize) return;
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

    @FXML
    void handleCheckBoxStock(ActionEvent event) {
        boolean enable = chkStock.isSelected();
        combCity.setDisable(!enable);
        combStore.setDisable(combCity.getSelectionModel().getSelectedItem().equals("Все города") || !enable);
        if (enable != stock) {
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

            if (stateInitialize && !city.equals(selectedItem)) {
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
        if (stateInitialize && !store.equals(selectedItem)) {
            btnOk.setDisable(false);
        } else {
            checkStatement();
        }
    }

    @FXML
    void handleCheckBoxNicSalt(ActionEvent event) {
        boolean selected = chkNicSalt.isSelected();
        if (selected) chkNicClassic.setSelected(false);
        if (stateInitialize && !nicSalt == selected) {
            btnOk.setDisable(false);
        } else {
            checkStatement();
        }
    }

    @FXML
    void handleCheckBoxNicClassic(ActionEvent event) {
        boolean selected = chkNicClassic.isSelected();
        if (selected) chkNicSalt.setSelected(false);
        if (stateInitialize && !nicClassic == selected) {
            btnOk.setDisable(false);
        } else {
            checkStatement();
        }
    }

    @FXML
    void handleDiscardButton() {
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
        String strengthStart = fieldStrengthStart.getText();
        String strengthEnd = fieldStrengthEnd.getText();
        String volumeStart = fieldVolumeStart.getText();
        String volumeEnd = fieldVolumeEnd.getText();
        String priceStart = fieldPriceStart.getText();
        String priceEnd = fieldPriceEnd.getText();

        int strengthS = strengthStart.equals("") ? -1 : Integer.parseInt(strengthStart);
        int strengthE = strengthEnd.equals("") ? -1 : Integer.parseInt(strengthEnd);
        int volumeS = volumeStart.equals("") ? -1 : Integer.parseInt(volumeStart);
        int volumeE = volumeEnd.equals("") ? -1 : Integer.parseInt(volumeEnd);
        int priceS = priceStart.equals("") ? -1 : Integer.parseInt(priceStart);
        int priceE = priceEnd.equals("") ? -1 : Integer.parseInt(priceEnd);

        /*TODO taste comb*/
        /*TODO nicotine checkboxes*/
        client.applyProductFilter(stockChecked, cityName, storeName, strengthS, strengthE, volumeS, volumeE, priceS, priceE);
        //updateStatement();
        fieldStrengthEnd.getScene().getWindow().hide();
    }

    @FXML
    void handleCancelButton() {
        chkNicClassic.getScene().getWindow().hide();
    }

    class FieldListener implements ChangeListener<String> {
        TextField field;
        TextField fieldToCompare;
        boolean lowerValue;

        FieldListener(TextField field, TextField fieldToCompare, boolean lowerValue) {
            this.field = field;
            this.fieldToCompare = fieldToCompare;
            this.lowerValue = lowerValue;
        }

        @Override
        public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
            if (!newValue.matches("\\d*")) {
                field.setText(newValue.replaceAll("[^\\d]", ""));
            }
            String fieldSelf = field.getText();
            String fieldComp = fieldToCompare.getText();

            if (!fieldSelf.equals("") && !fieldComp.equals("")) {
                int intField = Integer.parseInt(fieldSelf);
                int intComp = Integer.parseInt(fieldComp);
                if (lowerValue) {
                    if (intField > intComp) {
                        wrongRange();
                    } else {
                        validRange();
                    }
                } else {
                    if (intField < intComp) {
                        wrongRange();
                    } else {
                        validRange();
                    }
                }
            }
        }

        void wrongRange() {
            btnOk.setDisable(true);
            field.setStyle("-fx-text-fill: red; -fx-border-color: red;");
            fieldToCompare.setStyle("-fx-text-fill: red; -fx-border-color: red;");
        }

        void validRange() {
            field.setStyle(defaultFieldStyle);
            fieldToCompare.setStyle(defaultFieldStyle);
            checkStatement();
        }
    }
}
