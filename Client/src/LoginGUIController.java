import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private static final Logger LOGGER = LogManager.getLogger("ClientLogger");
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
        LOGGER.info("LoginStage initializing...");
        try {
            LOGGER.info("Loading preferences from config file...");
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
            LOGGER.error("Failed to load preferences: " + e.getMessage());
        }
    }

    @FXML
    void handleConnectButton() {
        LOGGER.info("Connect button handler");
        String ip = fieldIP.getText().trim();
        String port = fieldPort.getText().trim();
        String login = fieldLogin.getText().trim();
        String password = fieldPassword.getText().trim();
        boolean saveSettings = checkboxSaveSet.isSelected();
        lblError.setVisible(false);

        if (ip.length() > 0 && port.length() > 0 && login.length() > 0 && password.length() > 0) {
            LOGGER.info("Fields has a valid length");
            setDisableAll(true);
            client.connect(ip, port, login, password);
        } else {
            LOGGER.info("Invalid length of fields");
            showErrorLabel(ERROR_FIELDS);
        }
    }

    void authDenied() {
        LOGGER.info("Auth denied UI reaction");
        Platform.runLater(() -> {
            setDisableAll(false);
            showErrorLabel(AUTH_ERROR);
        });
    }

    void multiplySession(String nickname) {
        LOGGER.info("Multiply session UI reaction");
        Platform.runLater(() -> {
            setDisableAll(false);
            showAlertDialog(Alert.AlertType.ERROR, "Ошибка подключения!",
                    "Пользователь с ником " + nickname + " уже подключён!",
                    "Закройте все активные соединения и попробуйте заново").showAndWait();
        });
    }

    void setDisableAll(boolean state) {
        fieldIP.setDisable(state);
        fieldPort.setDisable(state);
        fieldLogin.setDisable(state);
        lblError.setDisable(state);
        fieldPassword.setDisable(state);
        checkboxSaveSet.setDisable(state);
        btnConnect.setDisable(state);
    }

    void failedToGetData() {
        LOGGER.info("Failed to get data");
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
        LOGGER.info("Connection failed");
        Platform.runLater(() -> {
            setDisableAll(false);
            showErrorLabel(CONNECTION_FAILED + " " + cause);
        });
    }

    void invalidPort(String port) {
        LOGGER.info("Invalid port");
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
