import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.ResourceBundle;

public class LoginGUIController {
    private final String ERROR_FIELDS = "Поля не могут быть пустыми!";
    private final String CONNECTION_FAILED = "Не удаётся установить соединение!";
    private final String INVALID_PORT = "Неверно указан порт!";
    private final String AUTH_ERROR = "Ошибка авторизации!";
    private final String CONFIG = "config.properties";
//    private final String CONFIG = "\\res\\config.properties";
    private Client client;

    @FXML
    private ResourceBundle resources;
    @FXML
    private URL location;
    @FXML
    private TextField fieldIP;
    @FXML
    private TextField fieldPort;
    @FXML
    private TextField fieldLogin;
    @FXML
    private Label lblError;
    @FXML
    private PasswordField fieldPassword;
    @FXML
    private CheckBox checkboxSaveSet;
    @FXML
    private Button btnConnect;

    @FXML
    void initialize() {
        try {
            //load settings from properties file
//            File props = new File(CONFIG);
//            if (!props.exists()){
//                props.createNewFile();
//            }
            Properties configProp = new Properties();
            configProp.load(getClass().getResourceAsStream(CONFIG));
//            configProp.load(new FileInputStream(props));
            boolean saveSetState = Boolean.valueOf(configProp.getProperty("saveSettings"));

            if (saveSetState) {
                checkboxSaveSet.setSelected(true);
                String login = configProp.getProperty("login");
                String password = configProp.getProperty("password");
                fieldLogin.setText(login);
                fieldPassword.setText(password);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleConnectButton() {
        String ip = fieldIP.getText().trim();
        String port = fieldPort.getText().trim();
        String login = fieldLogin.getText().trim();
        String password = fieldPassword.getText().trim();
        boolean saveSettings = checkboxSaveSet.isSelected();
        lblError.setVisible(false);

        if (ip.length() > 0 && port.length() > 0 && login.length() > 0 && password.length() > 0) {
            setDisableAll(true);
            client.connect(ip, port, login, password);
        } else {
            showErrorLabel(ERROR_FIELDS);
        }
    }

    void authDenied() {
        Platform.runLater(() -> {
            setDisableAll(false);
            showErrorLabel(AUTH_ERROR);
        });
    }

    void multiplySession(String nickname) {
        Platform.runLater(() -> {
            setDisableAll(false);
            showAlertDialog(Alert.AlertType.ERROR, "Ошибка подключения!",
                    "Пользователь с ником " + nickname + " уже подключён!",
                    "Закройте все активные соединения и попробуйте заново").showAndWait();
        });
    }

    private void setDisableAll(boolean state) {
        fieldIP.setDisable(state);
        fieldPort.setDisable(state);
        fieldLogin.setDisable(state);
        lblError.setDisable(state);
        fieldPassword.setDisable(state);
        checkboxSaveSet.setDisable(state);
        btnConnect.setDisable(state);
    }

    void failedToGetData() {
        Platform.runLater(() -> {
            setDisableAll(false);
            showAlertDialog(Alert.AlertType.ERROR, "Ошибка доступа!",
                    "В подключении отказано!",
                    "Недостаточый уровень прав!").showAndWait();
        });
    }

    void setClient(Client client) {
        this.client = client;
    }

    void connectionFailed(Throwable cause) {
        Platform.runLater(() -> {
            setDisableAll(false);
            showErrorLabel(CONNECTION_FAILED);
        });
    }

    void invalidPort(String port) {
        Platform.runLater(() -> {
            setDisableAll(false);
            showErrorLabel(INVALID_PORT);
        });
    }

    private void showErrorLabel(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
    }

    private Alert showAlertDialog(Alert.AlertType type, String title, String header, String context) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(context);
        return alert;
    }


}
