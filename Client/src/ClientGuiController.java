import java.net.Socket;
import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;


import javafx.application.Platform;
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

    @FXML
    private Label lblServerUpTime;

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
                    thread.sendMessage(Library.makeJsonString(Library.SERVER_INFO));
                } else if (data[1] == Library.DENIED) {
                    parent.authDenied();
                }
                break;
            case Library.SERVER_INFO:
                if (data[1] == Library.DENIED) {
                    thread.interrupt();
                    parent.getDataDenied();
                } else if (data[1] == Library.ACCEPTED) {
                    parent.getDataAccepted();
                    int access = Integer.parseInt(receivedData.getData());
                    tabUpdater.setDisable(false);
                    tabResearcher.setDisable(false);
                    tabProducts.setDisable(false);
                    if (access == Library.ADMIN) {
                        tabUsers.setDisable(false);
                        tabGrabber.setDisable(false);
                    }
                }
                break;
            case Library.START_TIME:
                updateServerUpTime(Long.valueOf(receivedData.getData()));

                //System.out.println(res2);

            case Library.PRODUCTS_COUNT:

            case Library.WAREHOUSES_COUNT:

            case Library.ACTIVE_USERS:

                break;
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

    private void updateServerUpTime(Long time) {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime serverStartTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());

        Duration duration = Duration.between(serverStartTime, currentTime);
        duration = duration.minusDays(duration.toDays()); // essentially "duration (mod 1 day)"
        Period period = Period.between(serverStartTime.toLocalDate(), currentTime.toLocalDate());

        int days = period.getDays();
        int hours = (int) duration.toHours() % 24;
        int minutes = (int) duration.toMinutes() % 60;

        Platform.runLater(() -> {
            lblServerUpTime.setText(parseToUptimeString(days, hours, minutes));
//            lblServerUpTime.setText(parseToUptimeString(15, 47, 18));
        });
    }

    private String parseToUptimeString(int days, int hours, int minutes) {
        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days);
            builder.append(" ");
            builder.append(getDeclensionWord(0, plurals(days)));
            builder.append(" ");
        }
        if (hours > 0) {
            builder.append(hours);
            builder.append(" ");
            builder.append(getDeclensionWord(1, plurals(hours)));
            builder.append(" ");
        }
        builder.append(minutes);
        builder.append(" ");
        builder.append(getDeclensionWord(2, plurals(minutes)));
        builder.append(" ");

        return builder.toString();
    }

    private Integer plurals(int n) {
        if (n == 0) return 0;
        n = Math.abs(n) % 100;
        int n1 = n % 10;
        if (n > 10 && n < 20) return 5;
        if (n1 > 1 && n1 < 5) return 2;
        if (n1 == 1) return 1;
        return 5;
    }

    private String getDeclensionWord(int type, int plurals) {
        //0 - days
        //1 - hours
        //2 - minutes
        switch (type) {
            case 0:
                switch (plurals) {
                    case 0:
                    case 5:
                        return "дней";
                    case 1:
                        return "день";
                    case 2:
                        return "дня";
                }
                break;
            case 1:
                switch (plurals) {
                    case 0:
                    case 5:
                        return "часов";
                    case 1:
                        return "час";
                    case 2:
                        return "часа";
                }
                break;
            case 2:
                switch (plurals) {
                    case 0:
                    case 5:
                        return "минут";
                    case 1:
                        return "минута";
                    case 2:
                        return "минуты";
                }
        }
        return null;
    }
}
