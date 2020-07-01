package com.guhar4k.client.gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginGUI {
    private static final String INVALID_LOGIN = "Поле логин не может-быть пустым!";
    private static final String INVALID_PASSWORD = "Поле пароля не может-быть пустым!";
    private GUIEvents listener;
    //private static final String ERROR_FIELDS = "Поля не могут быть пустыми!";
    private static final String INVALID_IP = "Неверно указан IP-аддрес!";
    private static final String CONNECTION_FAILED = "Не удаётся установить соединение!";
    private static final String INVALID_PORT = "Неверно указан порт!";
    private static final String AUTH_ERROR = "Ошибка авторизации!";
    private static final String CONFIG = "config.properties";
    private static final Logger LOGGER = LogManager.getLogger("ClientLogger");
    //private Controller controller;

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

//    @FXML
//    void initialize() {
//        LOGGER.info("LoginStage initializing...");
//        try {
//            LOGGER.info("Loading preferences from config file...");
//            //load settings from properties file
////            File props = new File(CONFIG);
////            if (!props.exists()){
////                props.createNewFile();
////            }
//            Properties configProp = new Properties();
//            configProp.load(getClass().getResourceAsStream(CONFIG));
////            configProp.load(new FileInputStream(props));
//            boolean saveSetState = Boolean.valueOf(configProp.getProperty("saveSettings"));
//
//            if (saveSetState) {
//                checkboxSaveSet.setSelected(true);
//                String login = configProp.getProperty("login");
//                String password = configProp.getProperty("password");
//                fieldLogin.setText(login);
//                fieldPassword.setText(password);
//            }
//        } catch (IOException e) {
//            LOGGER.error("Failed to load preferences: " + e.getMessage());
//        }
//    }

    void onConfigLoaded(String ip, String port, boolean saveSetState, String login, String password) {
        if (saveSetState) {
            fieldIP.setText(ip);
            fieldPort.setText(port);
            checkboxSaveSet.setSelected(true);
            fieldLogin.setText(login);
            fieldPassword.setText(password);
        }
    }

    void setListener(GUIEvents listener) {
        this.listener = listener;
    }

    @FXML
    void handleConnectButton() {
        LOGGER.info("Connect button handler");

        String ip = fieldIP.getText().trim();
        int dotsCount = ip.length() - ip.replace(".", "").length();

        if (ip.length() <= 0 || dotsCount != 3) {
            LOGGER.error("Invalid port");
            showErrorLabel(INVALID_IP);
            return;
        }

        int port;
        try {
            port = Integer.parseInt(fieldPort.getText().trim());

        } catch (NumberFormatException e) {
            LOGGER.error("Invalid port");
            showErrorLabel(INVALID_PORT);
            return;
        }

        String login = fieldLogin.getText().trim();
        String password = fieldPassword.getText().trim();
        if (login.length() == 0) {
            LOGGER.error("Invalid port");
            showErrorLabel(INVALID_LOGIN);
            return;
        } else if (password.length() == 0) {
            LOGGER.error("Invalid port");
            showErrorLabel(INVALID_PASSWORD);
            return;
        }

        lblError.setVisible(false);

        //TODO use saveSettingsField
        boolean saveSettings = checkboxSaveSet.isSelected();

        setDisableAll(true);
        listener.onConnectButtonEvent(ip, port, login, password);
        //controller.connect(ip, port, login, password);

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
            setDisableAll(false);
            showAlertDialog(Alert.AlertType.ERROR, "Ошибка подключения!",
                    "Пользователь с ником " + nickname + " уже подключён!",
                    "Закройте все активные соединения и попробуйте заново").showAndWait();
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

//    void setListener(Controller controller) {
//        this.controller = controller;
//    }

    void connectionFailed(String cause) {
        LOGGER.info("Connection failed");
        setDisableAll(false);
        showErrorLabel(cause);
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

    void authAccepted() {
        lblError.setText("Авторизован!");
    }
}
