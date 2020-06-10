import javafx.collections.ObservableList;
import main.*;
import main.product.Product;
import main.product.Warehouse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Client implements SocketThreadListener {
    private ClientGUI app;
    private LoginGUIController loginController;
    private ClientGUIController clientController;
    private SocketThread socketThread;
    private String login;
    private String password;
    private int updaterProductsTotal;
    private double updaterProgressPoint;
    private int researcherProductsTotal;
    private double researcherProgressPoint;
    private List<Warehouse> warehouses = new ArrayList<>();
    private Map<String, List<String>> someNewMap = new HashMap<>();
    private ArrayList<Product> products = new ArrayList<>();
    private boolean isConnected;
    private long serverStartTime;
    private Timer serverStartTimeTimer;
    private static final Logger LOGGER = LogManager.getLogger("ClientLogger");

    Client(ClientGUI app) {
        this.app = app;
    }

    void setLoginController(LoginGUIController loginController) {
        this.loginController = loginController;
    }

    void setClientController(ClientGUIController clientController) {
        this.clientController = clientController;
    }

    void connect(String ip, String port, String login, String password) {
        this.login = login;
        this.password = password;
        int numPort;
        try {
            numPort = Integer.valueOf(port);
        } catch (NumberFormatException e) {
            loginController.invalidPort(port);
            return;
        }
        try {
            LOGGER.info("Connecting to " + ip + ":" + port);
            Socket socket = new Socket(ip, numPort);
            socketThread = new SocketThread(this, login, socket);
        } catch (IOException e) {
            LOGGER.error("Failed to connect: " + e.getMessage());
            loginController.connectionFailed(e.getCause());
        }
    }

    private void auth(SocketThread thread) {
        LOGGER.info("Authorization request for " + login);
        thread.sendMessage(Library.getAuthRequest(login, password));
        login = null;
        password = null;
    }

    void disconnect() {
        LOGGER.info("Disconnecting from server...");
        isConnected = false;
        socketThread.close();
        if (serverStartTimeTimer != null) serverStartTimeTimer.cancel();
//        Platform.exit();
//        System.exit(0);
    }

    void startUpdater(boolean continueUpdate) {
        LOGGER.info("Start updater...");
        socketThread.sendMessage(Library.makeJsonString(Library.UPDATER, Library.START, String.valueOf(continueUpdate)));
    }

    void stopUpdater() {
        LOGGER.info("Stop updater...");
        socketThread.sendMessage(Library.makeJsonString(Library.UPDATER, Library.STOP));
    }

    void startResearcher() {
        LOGGER.info("Start researcher...");
        socketThread.sendMessage(Library.makeJsonString(Library.RESEARCHER, Library.START));
    }

    void stopResearcher() {
        LOGGER.info("Stop researcher...");
        socketThread.sendMessage(Library.makeJsonString(Library.RESEARCHER, Library.STOP));
    }

    void kickUser(String nickname) {
        LOGGER.info("Kick user request with nickname: " + nickname);
        socketThread.sendMessage(Library.makeJsonString(Library.USERS, Library.DISCONNECT, nickname));
    }

    List getStoreList(String selectedCity) {
        return someNewMap.get(selectedCity);
    }

    void showProductFilterStage(boolean inStockSelected, ObservableList<String> cityList, int selectedCity, ObservableList<String> storeList, int selectedStore) {
        app.showProductFilter(inStockSelected, cityList, selectedCity, storeList, selectedStore);
    }

    void applyProductFilter(boolean stock, String city, String store, int strengthStart, int strengthEnd, int volumeStart, int volumeEnd, int priceStart, int priceEnd) {
        LOGGER.info("Making product filter request...");
        if (!stock && city == null && store == null
                && strengthStart == -1 && strengthEnd == -1
                && volumeStart == -1 && volumeEnd == -1
                && priceStart == -1 && priceEnd == -1) {
            clientController.resetProductComboBoxes();
            return;
        } else {
            ProductRequest request;
            if (!stock) {
                request = new ProductRequest(false, -1, -1, strengthStart, strengthEnd, volumeStart, volumeEnd, priceStart, priceEnd);
            } else {
                int regionID = -1;
                int storeID = -1;

                if (city != null) {
                    for (Warehouse w : warehouses) {
                        if (w.getCity().equals(city)) {
                            regionID = w.getRegion();
                            break;
                        }
                    }
                }
                if (store != null) {
                    for (Warehouse w : warehouses) {
                        if (w.getAddress().equals(store)) {
                            storeID = w.getId();
                            break;
                        }
                    }
                }
                request = new ProductRequest(stock, regionID, storeID, strengthStart, strengthEnd, volumeStart, volumeEnd, priceStart, priceEnd);
            }
            socketThread.sendMessage(Library.productRequestToJson(request));
            clientController.updateProductComboBoxes(stock, city, store);
        }
    }

    //Socket events
    @Override
    public void onSocketThreadStart(SocketThread thread, Socket socket) {
        LOGGER.info("Client socket thread start");
    }

    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {
        LOGGER.info("Client socket thread ready");
        auth(thread);
    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, String msg) {
        DataProtocol receivedData = Library.jsonToObject(msg);

        byte[] header = receivedData.getHeader();
        switch (header[0]) {
            case Library.AUTH:
                if (header.length < 2) {
                    LOGGER.warn("Unknown auth message from server");
                    return;
                }
                if (header[1] == Library.ACCEPTED) {
                    isConnected = true;
                    String nickname = receivedData.getData();
                    clientController.setNickname(nickname);
                    thread.sendMessage(Library.makeJsonString(Library.SERVER_INFO));
                    LOGGER.info("Auth accepted with nickname " + nickname);
                } else if (header[1] == Library.DENIED) {
                    LOGGER.error("Auth denied");
                    loginController.authDenied();
                } else if (header[1] == Library.MULTIPLY_SESSION) {
                    LOGGER.error("Auth failed cause multiply session was found");
                    loginController.multiplySession(receivedData.getData());
                    loginController.setDisableAll(false);
                }
                break;
            case Library.WAREHOUSE_LIST:
                warehouses.add(Library.warehouseFromJson(receivedData.getData()));
                break;

            case Library.WAREHOUSE_LIST_END:
                LOGGER.info("Received warehouses list");
                ArrayList<Warehouse> list = new ArrayList<>(warehouses);
                int index = 0;
                while (list.size() > 0) {
                    int region = list.get(index).getRegion();
                    String city = list.get(index).getCity();
                    clientController.addCityToComb(city);
                    Iterator<Warehouse> iterator = list.iterator();

                    while (iterator.hasNext()) {
                        Warehouse warehouse = iterator.next();
                        if (warehouse.getRegion() == region) {
                            if (!someNewMap.containsKey(city)) {
                                List<String> tempList = new ArrayList<>();
                                tempList.add("Все магазины");
                                tempList.add(warehouse.getAddress());
                                someNewMap.put(city, tempList);
                            } else {
                                someNewMap.get(city).add(warehouse.getAddress());
                            }
                            iterator.remove();
                            index = 0;
                        }
                    }
                }
                break;

            case Library.SERVER_INFO:
                if (header[1] == Library.DENIED) {
                    LOGGER.error("Get server info request was denied");
                    loginController.failedToGetData();
                    thread.close();
                } else if (header[1] == Library.ACCEPTED) {
                    LOGGER.info("Get server info accepted");
                    app.showClient();
                    app.hideLoginStage();
                    int access = Integer.parseInt(receivedData.getData());
                    if (access == Library.ADMIN) {
                        LOGGER.info("User level - admin, unlock admin tabs");
                        if (clientController != null)
                            clientController.setTabsEnableForAdmin();
                    } else {
                        if (clientController != null){
                            LOGGER.info("User level - moderator, unlock moderator tabs");
                            clientController.setTabsEnableForModerator();
                        }
                    }
//                    try {
//                        Thread.sleep(1000);
//                        System.out.println();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                }
                break;
            case Library.START_TIME:
                serverStartTime = Long.valueOf(receivedData.getData());
                LOGGER.info("Received server start time: " + serverStartTime + ", setting up Timer for update Server UPTIME");
                clientController.setServerUpTime(parseServerTime(serverStartTime));
                serverStartTimeTimer = new Timer();
                serverStartTimeTimer.schedule(new TimeUpdater(), 0, 60000);
                break;
            case Library.PRODUCTS_COUNT:
                LOGGER.info("Received products count" );
                clientController.setProductsCount(receivedData.getData());
                break;
            case Library.WAREHOUSES_COUNT:
                LOGGER.info("Received warehouses count" );
                clientController.setWarehousesCount(receivedData.getData());
                break;
            case Library.USERS:
                switch (header[1]) {
                    case Library.COUNT:
                        LOGGER.info("Received connected users count");
                        clientController.setActiveUsersCount(receivedData.getData());
                        break;
                    case Library.LIST:
                        LOGGER.info("Received connected users list" );
                        clientController.updateUsersList(receivedData.getData().split(Library.DELIMITER));
                        break;
                    case Library.DISCONNECT:
                        String userNicknameDisconnet = receivedData.getData();
                        if (header.length > 2 && header[2] == Library.DENIED) {
                            LOGGER.error("Failed to kick user: " + userNicknameDisconnet );
                            clientController.failedToKickUser(userNicknameDisconnet);
                        } else {
                            LOGGER.info("Disconnected from the server by: " + userNicknameDisconnet);
                            clientController.kickedFromTheServer(receivedData.getData());
                            app.hideClientStage();
                        }
                        break;
                }
                break;
            case Library.UPDATER:
                switch (header[1]) {
                    case Library.DENIED:
                        LOGGER.error("ACCESS to UPDATER was DENIED by server");
                        break;
                    case Library.LAST_RUN:
                        LOGGER.info("Received updater last run");
                        clientController.setUpdaterLastRun(parseStringDate(receivedData.getData()));
                        break;
                    case Library.LAST_POSITION:
                        LOGGER.info("Received updater last position");
                        clientController.setLastPositionCheckboxVisible(Integer.parseInt(receivedData.getData()) > 0);
                        break;
                    case Library.START:
                        LOGGER.info("Received updater start info");
                        clientController.updaterStart();
                        break;
                    case Library.PROCESS_END:
                        LOGGER.info("Received updater stop info");
                        clientController.updaterStop();
                        break;
                    case Library.PRODUCTS_TOTAL:
                        LOGGER.info("Received updater products total");
                        updaterProductsTotal = Integer.parseInt(receivedData.getData());
                        updaterProgressPoint = (double) 1 / updaterProductsTotal;
                        break;
                    case Library.CURRENT:
                        LOGGER.info("Received current position of updater");
                        String[] arr = receivedData.getData().split(Library.DELIMITER);
                        int position = Integer.parseInt(arr[0]);
                        if (updaterProductsTotal == 0) {
                            updaterProductsTotal = Integer.parseInt(arr[1]);
                            updaterProgressPoint = (double) 1 / updaterProductsTotal;
                        }
                        clientController.setUpdaterProgress(updaterProgressPoint * position, position + "/" + updaterProductsTotal);
                        clientController.setUpdaterCurrentProduct(arr[2]);
                        break;
                    case Library.FOUND:
                        LOGGER.info("Received updater differences found");
                        String[] diffs = receivedData.getData().split(Library.DELIMITER);
                        clientController.setUpdatesFound(diffs[0]);
                        clientController.appendDifferencesFound(diffs[1]);
                        break;
                    case Library.FAILED:
                        LOGGER.info("Received updater failed to update product");
                        String[] failsMsg = receivedData.getData().split(Library.DELIMITER);
                        clientController.setUpdatesFailed(failsMsg[1]);
                        break;
                    case Library.EXCEPTION:
                        LOGGER.info("Received updater exception");
                        String[] exMsg = receivedData.getData().split(Library.DELIMITER);
                        if (exMsg.length >= 3) {
                            clientController.appendErrorToUpdaterLogger(exMsg[0], exMsg[1], exMsg[2]);
                        } else {
                            clientController.appendErrorToUpdaterLogger("SQLException", exMsg[0]);
                        }
                        break;
                }
                break;
            case Library.RESEARCHER:
                switch (header[1]) {
                    case Library.DENIED:
                        /*TODO - generate some alert dialog and close window*/
                        break;
                    case Library.LAST_RUN:
                        clientController.setResearcherLastUpdate(parseStringDate(receivedData.getData()));
                        break;
                    case Library.START:
                        clientController.researcherStart();
                        break;
                    case Library.PROCESS_END:
                        clientController.researcherEnd();
                        break;
                    case Library.PRODUCTS_TOTAL:
                        researcherProductsTotal = Integer.parseInt(receivedData.getData());
                        researcherProgressPoint = (double) 1 / researcherProductsTotal;
                        break;
                    case Library.CURRENT:
                        String[] arr = receivedData.getData().split(Library.DELIMITER);
                        int position = Integer.parseInt(arr[0]);
                        clientController.setResearcherProgress(researcherProgressPoint * position, position + "/" + researcherProductsTotal);
                        clientController.setResearcherCurrentGroup(arr[1]);
                        break;
                    case Library.CURRENT_CATEGORY:
                        String[] currCat = receivedData.getData().split(Library.DELIMITER);
                        clientController.setResearcherCurrentCategory(currCat[0] + "/" + currCat[1], currCat[2]);
                        break;
                    case Library.FOUND:
                        String[] diffs = receivedData.getData().split(Library.DELIMITER);
                        clientController.appendResearcherFoundProd(diffs[0]);
                        clientController.setResearcherTotalFounds(diffs[1]);
                        break;
//                    case Library.FAILED:
//                        String[] failsMsg = receivedData.getData().split(Library.DELIMITER);
//                        clientController.setResearchFailed(failsMsg[1]);
//                        break;
//                    case Library.EXCEPTION:
//                        String[] exMsg = receivedData.getData().split(Library.DELIMITER);
//                        if (exMsg.length >= 3) {
//                            clientController.appendErrorToResearcherLog(exMsg[0], exMsg[1], exMsg[2]);
//                        } else {
//                            clientController.appendErrorToUpdaterLogger("SQLException", exMsg[0]);
//                        }
//                        break;
                }
                break;
            case Library.PRODUCT_REQUEST:
                if (header[1] == Library.EMPTY) {
                    clientController.productsNotFound();
                }
                break;
            case Library.PRODUCT_LIST:
                products.add(Library.productFromJson(receivedData.getData()));
                break;
            case Library.PRODUCT_LIST_START:
                products.clear();
                break;
            case Library.PRODUCT_LIST_END:
                clientController.updateProductTableContent(products);
                break;
        }
    }

    @Override
    public void onSocketThreadStop(SocketThread thread) {
        LOGGER.info("Client socket thread stoped");
        if (isConnected) {
            LOGGER.error("Lost connection with server");
            clientController.connectionLost();
            app.hideClientStage();
            app.showLoginStage();
        }
        loginController.setDisableAll(false);
    }

    @Override
    public void onSocketThreadException(SocketThread thread, Exception e) {
        LOGGER.error("Socket thread exception: " + e.getMessage());
    }

    private String parseServerTime(Long time) {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime serverStartTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());
        Duration duration = Duration.between(serverStartTime, currentTime);

        int days = (int) duration.toDays();
        int hours = (int) duration.toHours() % 24;
        int minutes = (int) duration.toMinutes() % 60;
        return parseToUptimeString(days, hours, minutes);
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
                        return "минуту";
                    case 2:
                        return "минуты";
                }
        }
        return null;
    }

    private String parseStringDate(String date) {
        String[] arr = date.split("-");
        String pattern = "dd.MM.uuuu";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        int year = Integer.parseInt(arr[0]);
        int month = Integer.parseInt(arr[1]);
        int day = Integer.parseInt(arr[2]);
        LocalDate newDate = LocalDate.of(year, month, day);
        LocalDate currDate = LocalDate.now();

        if (newDate.isEqual(currDate)) return "сегодня";
        if (newDate.getYear() == currDate.getYear()
                && newDate.getMonth() == currDate.getMonth()
                && newDate.getDayOfMonth() + 1 == currDate.getDayOfMonth()) return "вчера";
        return newDate.format(formatter);
    }

    private class TimeUpdater extends TimerTask {
        @Override
        public void run() {
            clientController.setServerUpTime(parseServerTime(serverStartTime));
        }
    }
}
