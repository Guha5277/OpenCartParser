import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import main.Library;
import main.SocketThread;

import java.io.*;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import java.util.ResourceBundle;

public class LoginGuiController {
    private final String ERROR_FIELDS = "Поля не могут быть пустыми!";
    private final String CONNECTION_ERROR = "Не удаётся установить соединение!";
    private final String AUTH_ERROR = "Ошибка авторизации!";
    private final String CONFIG = "config.properties";
    Properties configProp;
    OutputStream configWriterStream;
    private Parent clientGuiRoot;
    private SocketThread clientSocketThread;
    private ClientGuiController clientGuiController;
    private String login;
    private String password;

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
        FXMLLoader clientGuiLoader = new FXMLLoader();
        clientGuiLoader.setLocation((getClass().getResource("client.fxml")));

        try {
            clientGuiRoot = clientGuiLoader.load();
            clientGuiController = clientGuiLoader.getController();
            clientGuiController.setParent(this);

            //load settings from properties file
            configProp = new Properties();
            configProp.load(getClass().getResourceAsStream(CONFIG));
            boolean saveSetState = Boolean.valueOf(configProp.getProperty("saveSettings"));

            if (saveSetState){
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
        login = fieldLogin.getText().trim();
        password = fieldPassword.getText().trim();
        boolean saveSettings = checkboxSaveSet.isSelected();
        lblError.setVisible(false);

        if (ip.length() > 0 && port.length() > 0 && login.length() > 0 && password.length() > 0) {
            setDisableAll(true);
            Socket socket = null;
            try {
                socket = new Socket(ip, Integer.parseInt(port));
            } catch (IOException e) {
                /*TODO Handle exception(logging)*/
                setDisableAll(false);
                showErrorLabel(CONNECTION_ERROR);
                return;
            }
            clientSocketThread = new SocketThread(clientGuiController, "ClientGUISocket", socket);

        } else {
            showErrorLabel(ERROR_FIELDS);
        }
    }

    private void showErrorLabel(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
    }


    void socketReady() {
        clientSocketThread.sendMessage(Library.getAuthRequest(login, password));
    }

    void getDataAccepted() {
        showClientWindow();
    }

    void getDataDenied() {
        Platform.runLater(() -> {
            showAlertDialog(Alert.AlertType.ERROR, "Access DENIED!",
                    "Невозможно получить доступ к запрашиваемой информации!",
                    "У вас недостаточный уровень прав!").showAndWait();
            setDisableAll(false);
        });
    }

    void authDenied() {
        clientSocketThread.close();
        Platform.runLater(() -> {
            setDisableAll(false);
            showErrorLabel(AUTH_ERROR);
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

    private void showClientWindow() {
        Platform.runLater(() -> {
            Scene scene = new Scene(clientGuiRoot);
            Stage clientStage = new Stage();
            clientStage.setScene(scene);
            Stage loginStage = (Stage) btnConnect.getParent().getScene().getWindow();
            loginStage.close();
            clientStage.setOnHidden(event -> {
                if (clientSocketThread != null && !clientSocketThread.isInterrupted()) {
                    clientSocketThread.close();
                    Platform.exit();
                }
            });
            clientStage.show();
        });
    }

    private Alert showAlertDialog(Alert.AlertType type, String title, String header, String context) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(context);
        return alert;
    }
}
