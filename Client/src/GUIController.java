import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class GUIController {
    private final String ERROR_FIELDS = "Поля не могут быть пустыми!";

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

    void initialize() {

    }

    @FXML
    void handleConnectButton(){
        String ip = fieldIP.getText().trim();
        String port = fieldPort.getText().trim();
        String login = fieldLogin.getText().trim();
        String password = fieldPassword.getText().trim();
        boolean saveSettings = checkboxSaveSet.isSelected();

        lblError.setVisible(false);

        if (ip.length() > 0 && port.length() > 0 && login.length() > 0 && password.length() > 0){

        } else {
            lblError.setText(ERROR_FIELDS);
            lblError.setVisible(true);
        }
    }
}
