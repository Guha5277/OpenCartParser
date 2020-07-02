package com.guhar4k.client.core;

import com.guhar4k.client.gui.AppGUI;
import com.guhar4k.client.gui.GUIEvents;
import com.guhar4k.library.DataProtocol;
import com.guhar4k.library.Library;
import com.guhar4k.library.ProductRequest;
import javafx.scene.image.Image;
import main.*;
import com.guhar4k.product.Product;
import com.guhar4k.product.Warehouse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Controller implements SocketThreadListener, GUIEvents {
    private ControllerEvents listener;

    private final String CONFIG_FILE = "/config.properties";
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
    private HashMap<Integer, byte[][]> images = new HashMap<>();
    private final String IMAGE_PATH = "Client/res/product_images/";

    public Controller(AppGUI app) {
        LOGGER.info("Controller constructor");
        listener = app;
    }

    //GUI Events
    @Override
    public void onGUIReady() {
        loadSettings();
    }

    @Override
    public void onConnectButtonEvent(String ip, int port, String login, String password) {
        connect(ip, port, login, password);
    }

    @Override
    public void onAppCloseRequest() {
        stop();
    }

    @Override
    public void startUpdaterRequest(boolean selected) {
        startUpdater(selected);
    }

    @Override
    public void stopUpdaterRequest() {
        stopUpdater();
    }

    @Override
    public void startResearcherRequest() {
        startResearcher();
    }

    @Override
    public void stopResearcherRequest() {
        stopResearcher();
    }

    @Override
    public void kickUserRequest(String nickname) {
        kickUser(nickname);
    }

    @Override
    public List storeListRequest(String selectedItem) {
        return getStoreList(selectedItem);
    }

    @Override
    public void applySettingsRequest(boolean updaterEnable, boolean researcherEnable, int updaterInterval, int researcherInterval, LocalTime updaterTime, LocalTime researcherTime) {
        applySettings(updaterEnable, researcherEnable, updaterInterval, researcherInterval, updaterTime, researcherTime);
    }

    @Override
    public void productsRequest(boolean stockChecked, String cityName, String storeName, int strengthStart, int strengthEnd, int volumeStart, int volumeEnd, int priceStart, int priceEnd) {
        applyProductFilter(stockChecked, cityName, storeName, strengthStart, strengthEnd, volumeStart, volumeEnd, priceStart, priceEnd);
    }

    @Override
    public void getImageRequest(int id) {
        getImage(id);
    }

    private void loadSettings() {
        String ip = "";
        String port = "";
        boolean saveSetState = false;
        String login = "";
        String password = "";

        try {
            Properties configProp = new Properties();
            configProp.load(getClass().getResourceAsStream(CONFIG_FILE));
            ip = configProp.getProperty("ip");
            port = configProp.getProperty("port");
            saveSetState = Boolean.valueOf(configProp.getProperty("saveSettings"));
            login = configProp.getProperty("login");
            password = configProp.getProperty("password");
        } catch (IOException e) {
            LOGGER.error("Failed to load client properties file");
        }
        listener.onLoginConfigLoaded(ip, port, saveSetState, login, password);
    }

    private void connect(String ip, int port, String login, String password) {
        this.login = login;
        this.password = password;

        try {
            LOGGER.info("Connecting to " + ip + ":" + port);
            Socket socket = new Socket(ip, port);
            socketThread = new SocketThread(this, login, socket);
        } catch (IOException e) {
            LOGGER.error("Failed to connect: " + e.getMessage());
            listener.onConnectFailed(e.getMessage());
        }
    }

    private void auth(SocketThread thread) {
        LOGGER.info("Authorization request for " + login);
        thread.sendMessage(Library.getAuthRequest(login, password));
        login = null;
        password = null;
    }

    private void stop() {
        LOGGER.info("Disconnecting from server...");
        isConnected = false;
        socketThread.close();
        if (serverStartTimeTimer != null) serverStartTimeTimer.cancel();
    }

    private void startUpdater(boolean continueUpdate) {
        LOGGER.info("Start updater...");
        socketThread.sendMessage(msgOf(header(Library.UPDATER, Library.START), String.valueOf(continueUpdate)));
    }

    private void stopUpdater() {
        LOGGER.info("Stop updater...");
        socketThread.sendMessage(msgOf(header(Library.UPDATER, Library.STOP)));
    }

    private void startResearcher() {
        LOGGER.info("Start researcher...");
        socketThread.sendMessage(msgOf(header(Library.RESEARCHER, Library.START)));
    }

    private void stopResearcher() {
        LOGGER.info("Stop researcher...");
        socketThread.sendMessage(msgOf(header(Library.RESEARCHER, Library.STOP)));
    }

    private void kickUser(String nickname) {
        LOGGER.info("Kick user request with nickname: " + nickname);
        socketThread.sendMessage(msgOf(header(Library.USERS, Library.DISCONNECT), nickname));
    }

    private void storeImageFirstChunk(String[] messageParts) {
        int productID = Integer.parseInt(messageParts[0]);
        int chunkCount = Integer.parseInt(messageParts[1]);
        String chunk = messageParts[2];

        byte[][] imageArray = new byte[chunkCount][];
        imageArray[0] = chunk.getBytes();
        images.put(productID, imageArray);
    }

    private void storeImageTransitChunk(String[] messageParts) {
        int productID = Integer.parseInt(messageParts[0]);
        int chunkIndex = Integer.parseInt(messageParts[1]);
        String chunk = messageParts[2];
        byte[][] imageArray = images.get(productID);
        imageArray[chunkIndex] = chunk.getBytes();
    }

    private void storeImageLastChunk(String[] messageParts) {
        int productID = Integer.parseInt(messageParts[0]);
        String chunk = messageParts[1];
        try {
            //store last part to map
            byte[][] imageArray = images.get(productID);
            imageArray[imageArray.length - 1] = chunk.getBytes();

            //decode byte arrays from the HashMap to the final result
            byte[] decodedImageBytes = decodeImage(joinByteArrays(imageArray));
            if (decodedImageBytes == null) {
                LOGGER.error("Failed to decode an image");
                return;
            }

            //write to disk
            FileOutputStream os = new FileOutputStream(IMAGE_PATH + productID + ".jpeg");
            os.write(decodedImageBytes);

            //Create an image instance
            Image image = new Image(new ByteArrayInputStream(decodedImageBytes));

            //remove image from map
            images.remove(productID);

            os.close();

            listener.onProductImageFound(productID, image);
        } catch (IOException e) {
            LOGGER.error("Failed to store an image: " + e.getMessage());
        }
    }

    private void storeFullImage(String[] messageParts) {
        int productID = Integer.parseInt(messageParts[0]);
        String chunk = messageParts[1];
        byte[] result = decodeImage(chunk.getBytes());
        try {
            //write to disk
            FileOutputStream os = new FileOutputStream(IMAGE_PATH + productID + ".jpeg");
            os.write(result);

            //Create an image instance
            Image image = new Image(new ByteArrayInputStream(result));

            //remove image from map
            images.remove(productID);

            os.close();
            listener.onProductImageFound(productID, image);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] joinByteArrays(byte[][] arrays) {
        byte[] result = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (byte[] b : arrays) {
            try {
                bos.write(b);
                result = bos.toByteArray();
                bos.close();
            } catch (IOException e) {
                LOGGER.error("Failed to join arrays");
            }

        }
        return result;
    }

    private byte[] decodeImage(byte[] encodedImage) {
        String encodedString = new String(encodedImage);

        //decode result
//        Base64.Decoder decoder = Base64.getDecoder();
//        return decoder.decode(encodedString);
        return Base64.getDecoder().decode(encodedImage);
    }

    private void noImageForProduct(String productID) {
        LOGGER.info("No available image for this product: " + productID);
        listener.onProductImageNotFound(productID);
    }

    private List getStoreList(String selectedCity) {
        return someNewMap.get(selectedCity);
    }


    private void applyProductFilter(boolean stock, String city, String store, int strengthStart, int strengthEnd, int volumeStart, int volumeEnd, int priceStart, int priceEnd) {
        LOGGER.info("Making product filter request...");
        if (!stock && city == null && store == null
                && strengthStart == -1 && strengthEnd == -1
                && volumeStart == -1 && volumeEnd == -1
                && priceStart == -1 && priceEnd == -1) {
            listener.noSelectedProducts();
            return;
        }

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
        listener.onProductRequestSent(stock, city, store);
    }

    private void applySettings(boolean updaterEnable, boolean researcherEnable, int updaterInterval, int researcherInterval, LocalTime updaterTime, LocalTime researcherTime) {
        LOGGER.info("Send new settings to the server");
        socketThread.sendMessage(msgOf(header(Library.UPDATER, Library.AUTOSTART), String.valueOf(updaterEnable)));
        socketThread.sendMessage(msgOf(header(Library.UPDATER, Library.AUTOSTART_INTERVAL), String.valueOf(updaterInterval)));
        socketThread.sendMessage(msgOf(header(Library.UPDATER, Library.AUTOSTART_TIME), String.valueOf(updaterTime)));
        socketThread.sendMessage(msgOf(header(Library.RESEARCHER, Library.AUTOSTART), String.valueOf(researcherEnable)));
        socketThread.sendMessage(msgOf(header(Library.RESEARCHER, Library.AUTOSTART_INTERVAL), String.valueOf(researcherInterval)));
        socketThread.sendMessage(msgOf(header(Library.RESEARCHER, Library.AUTOSTART_TIME), String.valueOf(researcherTime)));
    }

    //Getting an imageView from server or HW
    void getImage(int id) {
        LOGGER.info("Getting an image for product with id: " + id);
        String imagePath = IMAGE_PATH + id + ".jpeg";
        File file = new File(imagePath);
        if (file.exists()) {
            LOGGER.info("Image already on the disk, sending it to client...");
            Image image = new Image(file.toURI().toString());
            listener.onProductImageFound(id, image);
        } else {
            LOGGER.info("No image on the disk, sending request to the server");
            socketThread.sendMessage(msgOf(header(Library.IMAGE), String.valueOf(id)));
        }
    }

    //Socket events
    @Override
    public void onSocketThreadStart(SocketThread thread, Socket socket) {
        LOGGER.info("Controller socket thread start");
    }

    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {
        LOGGER.info("Controller socket thread ready");
        auth(thread);
    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, String msg) {
        DataProtocol receivedData = Library.jsonToObject(msg);

        byte[] header = receivedData.getHeader();
        switch (header[0]) {
            case Library.IMAGE:
                if (header.length > 1) {
                    switch (header[1]) {
                        case Library.EXCEPTION:
                            LOGGER.error("Failed to get image from the server. Product id: " + receivedData.getData());
                            break;
                        case Library.NO_IMAGE:
                            noImageForProduct(receivedData.getData());
                            break;
                        case Library.FIRST_CHUNK:
                            storeImageFirstChunk(receivedData.getData().split(Library.DELIMITER));
                            break;
                        case Library.TRANSIT_CHUNK:
                            /*TODO Check Map for contains an imageView*/ //if (!images.containsKey(Integer.valueOf(messageParts[0])));
                            storeImageTransitChunk(receivedData.getData().split(Library.DELIMITER));
                            break;
                        case Library.LAST_CHUNK:
                            storeImageLastChunk(receivedData.getData().split(Library.DELIMITER));
                            break;
                        case Library.FULL:
                            storeFullImage(receivedData.getData().split(Library.DELIMITER));
                            break;
                    }
                }
                break;

            case Library.AUTH:
                if (header.length < 2) {
                    LOGGER.warn("Unknown auth message from server");
                    return;
                }
                if (header[1] == Library.ACCEPTED) {
                    isConnected = true;
                    String nickname = receivedData.getData();
                    listener.onAuthAccepted(nickname);
                    thread.sendMessage(msgOf(header(Library.SERVER_INFO)));
                    LOGGER.info("Auth accepted with nickname " + nickname);
                } else if (header[1] == Library.DENIED) {
                    LOGGER.error("Auth denied");
                    listener.onAuthDenied();
                } else if (header[1] == Library.MULTIPLY_SESSION) {
                    LOGGER.error("Auth failed cause multiply session was found");
                    listener.onMultiplySession(receivedData.getData());
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

                    listener.onReceiveCityName(city);
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
                    listener.onFailToGetServerInfo();
                    thread.close();
                } else if (header[1] == Library.ACCEPTED) {
                    LOGGER.info("Get server info accepted");

                    int ACCESS_LEVEL = 0;
                    int SERVER_TIME = 1;
                    int PRODUCTS_COUNT = 2;
                    int WAREHOUSES_COUNT = 3;
                    int ACTIVE_USERS = 4;

                    String[] serverInfo = receivedData.getData().split(Library.DELIMITER);
                    boolean isAdmin = Integer.parseInt(serverInfo[ACCESS_LEVEL]) == Library.ADMIN;
                    serverStartTime = Long.valueOf(serverInfo[SERVER_TIME]);
                    int productsCount = Integer.parseInt(serverInfo[PRODUCTS_COUNT]);
                    int warehousesCount = Integer.parseInt(serverInfo[WAREHOUSES_COUNT]);
                    int clientsCount = Integer.parseInt(serverInfo[ACTIVE_USERS]);

                    listener.onServerInfoReceived(isAdmin, parseServerTime(serverStartTime), productsCount, warehousesCount, clientsCount);

                    serverStartTimeTimer = new Timer();
                    serverStartTimeTimer.schedule(new TimeUpdater(), 0, 60000);
                }
                break;
            case Library.USERS:
                switch (header[1]) {
                    case Library.COUNT:
                        LOGGER.info("Received connected users count");
                        listener.onActiveUsersCountChanged(receivedData.getData());
                        break;
                    case Library.LIST:
                        LOGGER.info("Received connected users list");
                        listener.onUsersListUpdated(receivedData.getData().split(Library.DELIMITER));
                        break;
                    case Library.DISCONNECT:
                        String userNicknameToKick = receivedData.getData();
                        if (header.length > 2 && header[2] == Library.DENIED) {
                            LOGGER.error("Failed to kick user: " + userNicknameToKick);
                            listener.onKickUserFailed(userNicknameToKick);
                        } else {
                            LOGGER.info("Disconnected from the server by: " + userNicknameToKick);
                            listener.kickedFromTheServer(receivedData.getData());
                        }
                        break;
                }
                break;
            case Library.UPDATER:
                switch (header[1]) {
                    case Library.INFO:
                        LOGGER.error("Received updater info");
                        int LAST_RUN = 0;
                        int AUTOSTART = 1;
                        int UPDATE_INTERVAL = 2;
                        int AUTOSTART_TIME = 3;
                        int LAST_POSITION = 4;

                        String[] updaterInfo = receivedData.getData().split(Library.DELIMITER);

                        String lastRun = parseStringDate(updaterInfo[LAST_RUN]);
                        boolean autostart = Boolean.parseBoolean(updaterInfo[AUTOSTART]);
                        int interval = Integer.parseInt(updaterInfo[UPDATE_INTERVAL]);
                        String[] time = updaterInfo[AUTOSTART_TIME].split(":");
                        LocalTime autostartTime = LocalTime.of(Integer.parseInt(time[0]), Integer.parseInt(time[1]));
                        boolean hasLastUpdatedPosition = Integer.parseInt(updaterInfo[LAST_POSITION]) > 0;

                        listener.onUpdaterInfoReceived(lastRun, autostart, interval, autostartTime, hasLastUpdatedPosition);
                        break;

                    case Library.LAST_RUN:
                        listener.onUpdaterLastRunChanged(parseStringDate(receivedData.getData()));
                        break;
                    case Library.LAST_POSITION:
                        listener.onUpdaterLastPositionChanged(Integer.parseInt(receivedData.getData()) > 0);
                        break;
                    case Library.DENIED:
                        LOGGER.error("ACCESS to UPDATER was DENIED by server");
                        break;

                    case Library.START:
                        LOGGER.info("Received updater start info");
                        listener.onUpdaterStart();
                        break;
                    case Library.PROCESS_END:
                        LOGGER.info("Received updater stop info");
                        listener.updaterStopped();
                        break;
                    case Library.PRODUCTS_TOTAL:
                        LOGGER.info("Received updater products total");
                        updaterProductsTotal = Integer.parseInt(receivedData.getData());
                        updaterProgressPoint = (double) 1 / updaterProductsTotal;
                        break;
                    case Library.CURRENT:
                        //LOGGER.info("Received current position of updater");
                        String[] arr = receivedData.getData().split(Library.DELIMITER);

                        int CURRENT = 0;
                        int TOTAL = 1;
                        int PRODUCT_NAME = 2;

                        int position = Integer.parseInt(arr[CURRENT]);
                        if (updaterProductsTotal == 0) {
                            updaterProductsTotal = Integer.parseInt(arr[TOTAL]);
                            updaterProgressPoint = (double) 1 / updaterProductsTotal;
                        }
                        listener.onUpdaterProgressChanged(updaterProgressPoint * position, position + "/" + updaterProductsTotal, arr[PRODUCT_NAME]);
                        break;
                    case Library.FOUND:
                        LOGGER.info("Received updater differences found");

                        int COUNT = 0;
                        int DIFFERENCES = 1;

                        String[] diffs = receivedData.getData().split(Library.DELIMITER);
                        listener.onUpdaterDifferencesFound(diffs[COUNT], diffs[DIFFERENCES]);
                        break;
                    case Library.FAILED:
                        LOGGER.info("Received updater failed to update product");
                        String[] failsMsg = receivedData.getData().split(Library.DELIMITER);

                        int URL = 0;
                        int FAILS_COUNT = 1;

                        listener.updaterError(failsMsg[URL], failsMsg[FAILS_COUNT]);
                        break;
                    case Library.EXCEPTION:
                        LOGGER.info("Updater exception");
                        String[] exMsg = receivedData.getData().split(Library.DELIMITER);
                        if (exMsg.length >= 3) {
                            listener.onUpdaterException(exMsg[0], exMsg[1], exMsg[2]);
                        } else {
                            listener.onUpdaterException("SQLException", exMsg[0]);
                        }
                        break;
                }
                break;
            case Library.RESEARCHER:
                switch (header[1]) {
                    case Library.INFO:
                        LOGGER.error("Received researcher info");
                        int LAST_RUN = 0;
                        int AUTOSTART = 1;
                        int RESEARCH_INTERVAL = 2;
                        int AUTOSTART_TIME = 3;

                        String[] researcherInfo = receivedData.getData().split(Library.DELIMITER);

                        String lastRun = parseStringDate(researcherInfo[LAST_RUN]);
                        boolean autostart = Boolean.parseBoolean(researcherInfo[AUTOSTART]);
                        int interval = Integer.parseInt(researcherInfo[RESEARCH_INTERVAL]);
                        String[] time = researcherInfo[AUTOSTART_TIME].split(":");
                        LocalTime autostartTime = LocalTime.of(Integer.parseInt(time[0]), Integer.parseInt(time[1]));

                        listener.onResearcherInfoReceived(lastRun, autostart, interval, autostartTime);
                        break;
                    case Library.DENIED:
                        LOGGER.error("ACCESS to RESEARCHER was DENIED by server");
                        break;

                    case Library.START:
                        LOGGER.info("Received researcher start info");
                        listener.onResearcherStart();
                        break;
                    case Library.PROCESS_END:
                        LOGGER.info("Received researcher stop info");
                        listener.onResearcherStopped();
                        break;
                    case Library.PRODUCTS_TOTAL:
                        //LOGGER.info("Received researcher products count");
                        researcherProductsTotal = Integer.parseInt(receivedData.getData());
                        researcherProgressPoint = (double) 1 / researcherProductsTotal;
                        break;
                    case Library.CURRENT:
                        //LOGGER.info("Received researcher current position");
                        String[] arr = receivedData.getData().split(Library.DELIMITER);
                        int POSITION = 0;
                        int CURRENT_GROUP = 1;
                        int position = Integer.parseInt(arr[POSITION]);
                        listener.onResearcherProgressChanged(researcherProgressPoint * position, position + "/" + researcherProductsTotal, arr[CURRENT_GROUP]);
                        break;
                    case Library.CURRENT_CATEGORY:
                        //LOGGER.info("Received researcher current category");
                        String[] currCat = receivedData.getData().split(Library.DELIMITER);
                        int CURRENT = 0;
                        int COUNT = 1;
                        int CATEGORY_NAME = 2;
                        listener.onResearcherCurrentCategoryChanged(currCat[CURRENT] + "/" + currCat[COUNT], currCat[CATEGORY_NAME]);
                        break;
                    case Library.FOUND:
                        LOGGER.info("Received researcher new position found");
                        String[] diffs = receivedData.getData().split(Library.DELIMITER);
                        int PRODUCT = 0;
                        int TOTAL_FOUND = 1;
                        listener.onResearcherNewProductFound(diffs[PRODUCT], diffs[TOTAL_FOUND]);
                        break;
//                    case Library.FAILED:
//                        String[] failsMsg = receivedData.getData().split(Library.DELIMITER);
//                        clientGUI.setResearchFailed(failsMsg[1]);
//                        break;
//                    case Library.EXCEPTION:
//                        String[] exMsg = receivedData.getData().split(Library.DELIMITER);
//                        if (exMsg.length >= 3) {
//                            clientGUI.appendErrorToResearcherLog(exMsg[0], exMsg[1], exMsg[2]);
//                        } else {
//                            clientGUI.appendErrorToUpdaterLogger("SQLException", exMsg[0]);
//                        }
//                        break;
                }
                break;
            case Library.PRODUCT_REQUEST:
                if (header[1] == Library.EMPTY) {
                    LOGGER.warn("No products found by user request");
                    listener.onProductsNotFound();
                }
                break;
            case Library.PRODUCT_LIST:
                products.add(Library.productFromJson(receivedData.getData()));
                listener.onProductFound(Library.productFromJson(receivedData.getData()));
                break;
            case Library.PRODUCT_LIST_START:
                products.clear();
                break;
            case Library.PRODUCT_LIST_END:
                LOGGER.warn("Received products list by user request");
                listener.allProductsReceived(products);
                break;
        }
    }

    @Override
    public void onSocketThreadStop(SocketThread thread) {
        LOGGER.info("Controller socket thread stopped");
        if (isConnected) {
            LOGGER.error("Lost connection with server");
            listener.onConnectLost();
//            clientGUI.connectionLost();
//            app.hideClientStage();
//            app.showLoginStage();
        }
//        loginGUI.setDisableAll(false);
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

    private String msgOf(byte[] header, String... data) {
        return Library.makeJsonString(header, data);
    }

    private byte[] header(byte... header) {
        return header;
    }

    private class TimeUpdater extends TimerTask {
        @Override
        public void run() {
            LOGGER.info("TIMER TASK");
            listener.onServerUptimeUpdated(parseServerTime(serverStartTime));
        }
    }
}
