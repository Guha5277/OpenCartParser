import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;


import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import main.DataProtocol;
import main.Library;
import main.SocketThread;
import main.SocketThreadListener;


public class ClientGuiController implements SocketThreadListener {
    private LoginGuiController parent;
    @FXML
    private ResourceBundle resources;
    @FXML
    private URL location;
    @FXML
    private Label lblNickname;
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

    @Override
    public void onSocketThreadStart(SocketThread thread, Socket socket) {
        System.out.println("ClientGUISocket start");
    }

    @Override
    public void onSocketThreadStop(SocketThread thread) {
        System.out.println("ClientGUISocket socket stop");
    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, String msg) {
        DataProtocol receivedData = Library.jsonToObject(msg);

        byte[] data = receivedData.getHeader();
        switch (data[0]) {
            case Library.AUTH:
                if (data.length < 2) {
                    /*TODO show message error (unknown message from server)*/
                    return;
                }
                if (data[1] == Library.ACCEPTED) {
                    //parent.authAccept();
                    lblNickname.setText(receivedData.getData());
                    thread.sendMessage(Library.makeJsonString(Library.GET_SERVER_INFO));
                } else if (data[1] == Library.DENIED) {
                    parent.authDenied();
                }
                break;
            case Library.GET_SERVER_INFO:
                if (data[1] == Library.DENIED) {
                    thread.interrupt();
                    parent.getDataDenied();
                } else if (data[1] == Library.ACCEPTED) {
                    parent.getDataAccepted();
                    int access = Integer.parseInt(receivedData.getData());
                    tabUpdater.setDisable(false);
                    tabResearcher.setDisable(false);
                    tabProducts.setDisable(false);
                    if (access == Library.ADMIN){
                        tabUsers.setDisable(false);
                        tabGrabber.setDisable(false);
                    }
                }
        }
    }

    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {
        System.out.println("ClientGUISocket ready");
        parent.socketReady();
    }

    @Override
    public void onSocketThreadException(SocketThread thread, Exception e) {
        System.out.println("ClientGUISocket exception: " + e.getMessage() + " " + e.getCause());
    }

    void setParent(LoginGuiController controller) {
        parent = controller;
    }
}
