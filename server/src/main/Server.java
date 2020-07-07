package main;

import com.guhar4k.library.DataProtocol;
import com.guhar4k.library.Library;
import com.guhar4k.library.ProductRequest;
import com.guhar4k.parser.*;
import com.guhar4k.product.Product;
import com.guhar4k.product.Warehouse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

public class Server implements ServerSocketThreadListener, SocketThreadListener, IUpdater, IResearcher, IGrabber {
    private long serverStartAt;
    private int productsCount;
    private int warehousesCount;
    private int updaterTotalProd;
    private final String UPDATER = "updater";
    private final String RESEARCHER = "researcher";
    private LocalDate updaterLastRunDate;
    private LocalDate researcherLastRunDate;
    private int lastUpdatedProductPosition;
    private boolean updaterAutostartState;
    private boolean researcherAutostartState;
    private LocalTime updaterAutostartTime;
    private LocalTime researcherAutostartTime;
    private int updaterDaysInterval;
    private int researcherDaysInterval;
    private Timer updaterTimer = new Timer();
    private Timer researcherTimer = new Timer();
    private Thread updaterThread;
    private Thread researcherThread;
    private Updater updater;
    private List<Warehouse> warehouses;
    private Researcher researcher;
    private Vector<SocketThread> clients = new Vector<>();
    private static final Logger SERVER_LOGGER = LogManager.getLogger("ServerLogger");
    private static final Logger USERS_LOGGER = LogManager.getLogger("UsersLogger");
    private final String IMAGES_PATH = "Server/res/images/";
    private final int IMAGE_CHUNK_LIMIT = 30000;

    public static void main(String[] args) {
        new Server();
    }

    private Server() {
        new ServerSocketThread(this, "server", 5277, 200);
        serverStartAt = System.currentTimeMillis();
    }

    private void initialize() {
        SERVER_LOGGER.info("Server initializing...");
        SQLClient.connect();
        productsCount = SQLClient.getProductsCount();
        warehousesCount = SQLClient.getWarehousesCount();
        updaterLastRunDate = SQLClient.getProcessLastRun(UPDATER);
        updaterAutostartTime = SQLClient.getProcessLaunchTime(UPDATER);
        updaterAutostartState = SQLClient.getProcessAutoStartState(UPDATER);
        updaterDaysInterval = SQLClient.getProcessDayInterval(UPDATER);

        researcherLastRunDate = SQLClient.getProcessLastRun(RESEARCHER);
        researcherAutostartTime = SQLClient.getProcessLaunchTime(RESEARCHER);
        researcherAutostartState = SQLClient.getProcessAutoStartState(RESEARCHER);
        researcherDaysInterval = SQLClient.getProcessDayInterval(RESEARCHER);
        lastUpdatedProductPosition = SQLClient.getLastUpdatedProductPosition();

        if (updaterAutostartState) {
            setUpdaterTimerTask();
        }

        if (researcherAutostartState) {
            setResearcherTimerTask();
        }

        String address = "";
        String name = "";
        try {
            address = InetAddress.getLocalHost().getHostAddress();
            name = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        SERVER_LOGGER.info("Products: " + productsCount);
        SERVER_LOGGER.info("Warehouses: " + warehousesCount);
        SERVER_LOGGER.info("Server Started " + name + " " + address);
    }

    private String msgOf(byte[] header, String... data) {
        return Library.makeJsonString(header, data);
    }

    private byte[] header(byte... header) {
        return header;
    }

    private List<Product> getProductsByFilter(ProductRequest request) {
        String query = makeProductQuery(request);
        return SQLClient.getProductsListByQuery(query);
    }

    private String makeProductQuery(ProductRequest request) {
        StringBuilder query = new StringBuilder();
        boolean hasWhere = false;
        boolean stockRequired = request.isInStock();
        String delimiter;
        if (stockRequired) {
            delimiter = " AND";
            int regionID = request.getRegionID();
            int storeID = request.getStoreID();

            boolean singleStore = false;
            String stockFilter;

            if (regionID == -1 && storeID == -1) {
                stockFilter = " AND product_remains.remains > 0";
            } else if (storeID != -1) {
                stockFilter = " AND product_remains.remains > 0 AND warehouse.id=" + storeID;
                singleStore = true;
            } else {
                stockFilter = " AND product_remains.remains > 0 AND warehouse.region=" + regionID;
            }

            if (singleStore) {
                query.append("SELECT liquids.id, liquids.name, liquids.price, liquids.volume, liquids.strength, liquids.category, liquids.url, product_remains.remains from product_remains ");
            } else {
                query.append("SELECT liquids.id, liquids.name, liquids.price, liquids.volume, liquids.strength, liquids.category, liquids.url, product_remains.remains, warehouse.id, warehouse.address from product_remains ");
            }
            query.append("inner join warehouse on product_remains.warehouse_id = warehouse.id ");
            query.append("inner join liquids on product_remains.product_id = liquids.id");
            query.append(stockFilter);

        } else {
            delimiter = " WHERE";
            query.append("SELECT id, name, price, volume, strength, category, url from liquids");
        }
        int strengthStart = request.getStrengthStart();
        int strengthEnd = request.getStrengthEnd();
        int volumeStart = request.getVolumeStart();
        int volumeEnd = request.getVolumeEnd();
        int priceStart = request.getPriceStart();
        int priceEnd = request.getPriceEnd();

        if (strengthStart > -1 || strengthEnd > -1) {
            hasWhere = true;
            query.append(delimiter);
            query.append(" strength");
            if (strengthStart > -1 && strengthEnd > -1) {
                query.append(" BETWEEN ");
                query.append(strengthStart);
                query.append(" AND ");
                query.append(strengthEnd);
            } else if (strengthStart > -1) {
                query.append(" > ");
                query.append(strengthStart);
            } else {
                query.append(" < ");
                query.append(strengthEnd);
            }
        }

        if (volumeStart > -1 || volumeEnd > -1) {
            if (hasWhere) {
                query.append(" AND volume");
            } else {
                hasWhere = true;
                query.append(" WHERE volume");
            }
            if (volumeStart > -1 && volumeEnd > -1) {
                query.append(" BETWEEN ");
                query.append(volumeStart);
                query.append(" AND ");
                query.append(volumeEnd);
            } else if (volumeStart > -1) {
                query.append(" > ");
                query.append(volumeStart);
            } else {
                query.append(" < ");
                query.append(volumeEnd);
            }
        }

        if (priceStart > -1 || priceEnd > -1) {
            if (hasWhere) {
                query.append(" AND price");
            } else {
                query.append(" WHERE price");
            }
            if (priceStart > -1 && priceEnd > -1) {
                query.append(" BETWEEN ");
                query.append(priceStart);
                query.append(" AND ");
                query.append(priceEnd);
            } else if (priceStart > -1) {
                query.append(" > ");
                query.append(priceStart);
            } else {
                query.append(" < ");
                query.append(priceEnd);
            }
        }
        return query.toString();
    }

    private void sendProductList(List<Product> list, ClientThread client) {
        int listSize = list.size();
        if (listSize == 0) {
            SERVER_LOGGER.info("No result found for PRODUCT_REQUEST");
            sendMsgToModeratorsAndAdmins(msgOf(header(Library.PRODUCT_REQUEST, Library.EMPTY)));
            return;
        }

        SERVER_LOGGER.info(listSize + " products found for PRODUCT_REQUEST");

        client.sendMessage(msgOf(header(Library.PRODUCT_LIST_START)));

        for (Product p : list) {
            client.sendMessage(Library.productToJson(p));
        }
        client.sendMessage(msgOf(header(Library.PRODUCT_LIST_END)));

        SERVER_LOGGER.info(listSize + " products sent to client successful");
    }

    private void sendWarehousesList(SocketThread thread) {
        warehouses = SQLClient.getAllWarehouses();
        if (warehouses == null) {
            SERVER_LOGGER.error("No warehouses list found in DB");
            return;
        }
        for (Warehouse w : warehouses) {
            thread.sendMessage(Library.warehouseToJson(w));
        }
        thread.sendMessage(msgOf(header(Library.WAREHOUSE_LIST_END)));
    }

    private void authorizeClient(ClientThread clientThread, String userData) {
        String[] data = userData.split(Library.DELIMITER);
        String login = data[0];
        String password = data[1];
        USERS_LOGGER.info("AUTH REQUEST for " + login);

        String nickname = SQLClient.getNickname(login, password);
        if (nickname == null) {
            USERS_LOGGER.error("AUTH FAILED for " + login + "! Invalid login/password");
            clientThread.authFailed(msgOf(header(Library.AUTH, Library.DENIED)));
            clientThread.close();
        } else {
            ClientThread client = findUserByNickname(nickname);
            if (client == null) {
                clientThread.authAccept(nickname);
                clientThread.sendMessage(msgOf(header(Library.AUTH, Library.ACCEPTED), nickname));
                int userAccessLevel = SQLClient.getUserRole(nickname);
                clientThread.setAccessLevel(userAccessLevel);
                USERS_LOGGER.info("AUTH SUCCESS for " + login + " with nickname: + " + nickname);
            } else {
                client.sendMessage(msgOf(header(Library.AUTH, Library.MULTIPLY_SESSION), client.getNickname()));
                clientThread.close();
                USERS_LOGGER.error("AUTH FAILED for " + login + "! Client connected already");
            }
        }
    }

    private void startUpdater(boolean continueUpdate) {
        if (updaterThread != null && updaterThread.isAlive()) return;
        if (continueUpdate && lastUpdatedProductPosition < productsCount) {
            updater = new Updater(this, lastUpdatedProductPosition, IMAGES_PATH, SQLClient.getAllProducts(), SQLClient.getAllWarehouses());
        } else {
            updater = new Updater(this, 0, IMAGES_PATH, SQLClient.getAllProducts(), SQLClient.getAllWarehouses());
        }
        updaterThread = new Thread(updater);
        updaterThread.start();
        SERVER_LOGGER.info("Updater started");
    }

    private void stopUpdater() {
        if (updaterThread == null || !updaterThread.isAlive()) return;
        //updaterThread.interrupt();
        updater.stop();
        updaterThread = null;
        updater = null;
        SERVER_LOGGER.info("Updater stopped");
    }

    private void startResearcher() {
        if (researcherThread != null && updaterThread.isAlive()) return;
        researcher = new Researcher("https://ilfumoshop.ru/zhidkost-dlya-zapravki-vejporov", this);
        researcherThread = new Thread(researcher);
        researcherThread.start();
        SERVER_LOGGER.info("Researcher started");
    }

    private void stopResearcher() {
        if (researcherThread == null || !researcherThread.isAlive()) return;
        researcherThread.interrupt();
        researcher.stop();
        researcherThread = null;
        researcher = null;
        SERVER_LOGGER.info("Researcher stopped");
    }

    private ClientThread findUserByNickname(String nickname) {
        for (SocketThread thread : clients) {
            ClientThread client = (ClientThread) thread;
            if (!client.isAuthorized()) continue;
            if (client.getNickname().equals(nickname))
                return client;
        }
        return null;
    }

    private void sendMsgToModeratorsAndAdmins(String msg) {
        if (clients.size() == 0) return;
        for (SocketThread thread : clients) {
            ClientThread client = (ClientThread) thread;
            if (!client.isAuthorized() || client.getAccessLevel() > Library.MODERATOR) continue;
            client.sendMessage(msg);
        }
    }

    private String getListOfClients() {
        StringBuilder sb = new StringBuilder();
        for (SocketThread thread : clients) {
            ClientThread client = (ClientThread) thread;
            if (!client.isAuthorized()) continue;
            sb.append(client.getNickname());
            sb.append(Library.DELIMITER);
        }
        return sb.toString();
    }

    private void sendImageToClient(ClientThread client, int productID, String imagePath) {
        //Test send images
        File file = new File(imagePath);
        try {
            Base64.Encoder encoder = Base64.getEncoder();
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String encodeString = encoder.encodeToString(fileContent);
            byte[] encodedFileContent = encodeString.getBytes();
            int imageLength = encodedFileContent.length;

            SERVER_LOGGER.info("Image length: " + imageLength);

            if (imageLength > IMAGE_CHUNK_LIMIT) {
                SERVER_LOGGER.info("Image length is out of limit (" + IMAGE_CHUNK_LIMIT + ")");
                int subArraysCount = imageLength / IMAGE_CHUNK_LIMIT;
                if (imageLength % IMAGE_CHUNK_LIMIT != 0) {
                    subArraysCount++;
                }
                SERVER_LOGGER.info("Image chunks: " + subArraysCount);

                int indexStart = 0;
                byte[][] subArrays = new byte[subArraysCount][];
                for (int i = 0; i < subArraysCount; i++) {
                    SERVER_LOGGER.info("Create chink #" + (i + 1));
                    int indexEnd = IMAGE_CHUNK_LIMIT * (i + 1);
                    subArrays[i] = Arrays.copyOfRange(encodedFileContent, indexStart, indexEnd < imageLength ? indexEnd : imageLength);
                    String data;
                    String jsonMsg;
                    if (i == 0) {
                        //Image first chunk
                        data = productID + Library.DELIMITER + subArraysCount + Library.DELIMITER + new String(subArrays[i]);
                        jsonMsg = msgOf(header(Library.IMAGE, Library.FIRST_CHUNK), data);
                    } else if (i == subArraysCount - 1) {
                        //Image last chunk
                        data = productID + Library.DELIMITER + new String(subArrays[i]);
                        jsonMsg = msgOf(header(Library.IMAGE, Library.LAST_CHUNK), data);
                    } else {
                        //Image transit chunk
                        data = productID + Library.DELIMITER + i + Library.DELIMITER + new String(subArrays[i]);
                        jsonMsg = msgOf(header(Library.IMAGE, Library.TRANSIT_CHUNK), data);
                    }
                    indexStart = indexEnd;
                    //SERVER_LOGGER.info("Send chink #" + (i+1));
                    client.sendMessage(jsonMsg);
                }
            } else {
                client.sendMessage(msgOf(header(Library.IMAGE, Library.FULL), productID + Library.DELIMITER + encodeString));
            }
            SERVER_LOGGER.info("Successful sent image " + imagePath + " to client " + (client.getNickname() == null ? "Anonymous" : client.getNickname()));
        } catch (IOException e) {
            SERVER_LOGGER.error("Failed to send image " + imagePath + " to client " + (client.getNickname() == null ? "Anonymous" : client.getNickname()));
        }
    }

    //Server Events
    @Override
    public void onThreadStart(ServerSocketThread thread) {
        SERVER_LOGGER.info("Server Thread Started");
    }

    @Override
    public void onServerStart(ServerSocketThread thread, ServerSocket server) {
        initialize();
    }

    @Override
    public void onSocketAccepted(ServerSocket server, Socket socket) {
        SERVER_LOGGER.info("Client Socket accepted: " + socket.getInetAddress());
        new ClientThread(this, "client", socket);
    }

    @Override
    public void onServerException(ServerSocketThread thread, Exception e) {
        SERVER_LOGGER.error("Exception: " + e.getMessage());
    }

    @Override
    public void onThreadStop(ServerSocketThread thread) {
        SERVER_LOGGER.info("Server Thread Stopped");
        SQLClient.disconnect();
    }

    //SocketEvents
    @Override
    public void onSocketThreadStart(SocketThread thread, Socket socket) {
        SERVER_LOGGER.info("Client SocketThread started " + socket.getInetAddress());
    }

    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {
        SERVER_LOGGER.info("Client SocketThread ready and added to list" + socket.getInetAddress());
        clients.add(thread);
    }

    @Override
    public void onSocketThreadStop(SocketThread thread) {
        ClientThread clientThread = (ClientThread) thread;
        clients.remove(thread);
        sendMsgToModeratorsAndAdmins(msgOf(header(Library.USERS, Library.COUNT), String.valueOf(clients.size())));
        sendMsgToModeratorsAndAdmins(msgOf(header(Library.USERS, Library.LIST), getListOfClients()));
        USERS_LOGGER.info("Client disconnected: " + clientThread.getNickname());
    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, String msg) {
        SERVER_LOGGER.info("Received message from client (" + msg.length() + ")");
        ClientThread client = (ClientThread) thread;
        DataProtocol receivedData;
        try{
            receivedData = Library.jsonToObject(msg);
        } catch (com.google.gson.JsonSyntaxException e){
            SERVER_LOGGER.error("Failed to parse JSON from string: " + e.getMessage());
            return;
        }

        byte[] header = receivedData.getHeader();
        /*TODO check client for authorize and get client access level*/
        switch (header[0]) {
            case Library.AUTH:
                if (header.length < 2 || header[1] != Library.REQUEST) {
//                    client.msgFormatError(msgOf()(Library.MESSAGE_FORMAT_ERROR));
                    client.sendMessage(msgOf(header(Library.MESSAGE_FORMAT_ERROR)));
                    USERS_LOGGER.error("AUTH FAILED cause MESSAGE FORMAT ERROR header length: " + header.length);
                    return;
                }
                authorizeClient(client, receivedData.getData());
                break;
            case Library.SERVER_INFO:
                if (!client.isAuthorized()) {
                    USERS_LOGGER.error("GET SERVER_INFO FAILED cause client is not authorized!");
//                    client.sendMessage(msgOf()(Library.SERVER_INFO, Library.DENIED));
                    client.sendMessage(msgOf(header(Library.SERVER_INFO, Library.DENIED)));
                    clients.remove(client);
                    client.close();
                } else {
                    int accessLevel = client.getAccessLevel();
                    if (accessLevel == ClientThread.ADMIN || accessLevel == ClientThread.MODERATOR) {
                        USERS_LOGGER.info("GET SERVER_INFO ACCEPTED for " + client.getNickname());
                        String serverInfo = accessLevel + Library.DELIMITER +
                                serverStartAt + Library.DELIMITER +
                                productsCount + Library.DELIMITER +
                                warehousesCount + Library.DELIMITER +
                                clients.size();
//                        client.sendMessage(msgOf()(Library.SERVER_INFO, Library.ACCEPTED, serverInfo));
                        client.sendMessage(msgOf(header(Library.SERVER_INFO, Library.ACCEPTED), serverInfo));

                        String updaterInfo = updaterLastRunDate + Library.DELIMITER +
                                updaterAutostartState + Library.DELIMITER +
                                updaterDaysInterval + Library.DELIMITER +
                                updaterAutostartTime + Library.DELIMITER +
                                lastUpdatedProductPosition;
//                        client.sendMessage(msgOf()(Library.UPDATER, Library.INFO, updaterInfo));
                        client.sendMessage(msgOf(header(Library.UPDATER, Library.INFO), updaterInfo));

                        String researcherInfo = researcherLastRunDate + Library.DELIMITER +
                                researcherAutostartState + Library.DELIMITER +
                                researcherDaysInterval + Library.DELIMITER +
                                researcherAutostartTime;
                        client.sendMessage(msgOf(header(Library.RESEARCHER, Library.INFO), researcherInfo));
                        sendWarehousesList(thread);

                        sendMsgToModeratorsAndAdmins(msgOf(header(Library.USERS, Library.COUNT), String.valueOf(clients.size())));
                        sendMsgToModeratorsAndAdmins(msgOf(header(Library.USERS, Library.LIST), getListOfClients()));


                    } else {
                        USERS_LOGGER.error("GET SERVER_INFO FAILED cause client access level is lower than required: " + accessLevel);
                        client.sendMessage(msgOf(header(Library.SERVER_INFO, Library.DENIED)));
                    }
                }
                break;
            case Library.UPDATER:
                USERS_LOGGER.info("UPDATER...");
                int accessLevel = client.getAccessLevel();
                String nickname = client.getNickname();
                if (accessLevel > 2) {
                    USERS_LOGGER.error("ACCESS TO UPDATER DENIED cause client access level is lower than required: " + accessLevel);
                    client.sendMessage(msgOf(header(Library.UPDATER, Library.DENIED)));
                    return;
                }
                switch (header[1]) {
                    case Library.START:
                        USERS_LOGGER.info("start UPDATER request by " + nickname);
                        startUpdater(Boolean.valueOf(receivedData.getData()));
                        break;
                    case Library.STOP:
                        USERS_LOGGER.info("stop UPDATER request by " + nickname);
                        stopUpdater();
                        break;
                    case Library.AUTOSTART:
                        updaterAutostartState = Boolean.valueOf(receivedData.getData());
                        SQLClient.updateProcessAutoStartState(UPDATER, updaterAutostartState);
                        break;
                    case Library.AUTOSTART_INTERVAL:
                        updaterDaysInterval = Integer.parseInt(receivedData.getData());
                        SQLClient.updateProcessDayInterval(UPDATER, updaterDaysInterval);
                        break;
                    case Library.AUTOSTART_TIME:
                        updaterAutostartTime = LocalTime.parse(receivedData.getData());
                        SQLClient.updateProcessStartTime(UPDATER, updaterAutostartTime);
                        break;
                }
                break;
            case Library.RESEARCHER:
                if (client.getAccessLevel() > 2) {
                    USERS_LOGGER.error("ACCESS TO RESEARCHER DENIED cause client access level is lower than required: " + client.getAccessLevel());
                    client.sendMessage(msgOf(header(Library.RESEARCHER, Library.DENIED)));
                    clients.remove(client);
                    client.close();
                    return;
                }
                switch (header[1]) {
                    case Library.START:
                        USERS_LOGGER.info("Start RESEARCHER by " + client.getNickname());
                        startResearcher();
                        break;
                    case Library.STOP:
                        USERS_LOGGER.info("Stop RESEARCHER by " + client.getNickname());
                        stopResearcher();
                        break;
                    case Library.AUTOSTART:
                        researcherAutostartState = Boolean.valueOf(receivedData.getData());
                        SQLClient.updateProcessAutoStartState(RESEARCHER, researcherAutostartState);
                        break;
                    case Library.AUTOSTART_INTERVAL:
                        researcherDaysInterval = Integer.parseInt(receivedData.getData());
                        SQLClient.updateProcessDayInterval(RESEARCHER, researcherDaysInterval);
                        break;
                    case Library.AUTOSTART_TIME:
                        researcherAutostartTime = LocalTime.parse(receivedData.getData());
                        SQLClient.updateProcessStartTime(RESEARCHER, researcherAutostartTime);
                        break;
                }
                break;
            case Library.USERS:
                USERS_LOGGER.info("USER MODERATION...");
                switch (header[1]) {
                    case Library.DISCONNECT:
                        String nicname2 = receivedData.getData();
                        int initiatorLvl = client.getAccessLevel();
                        ClientThread clientShouldBeKicked = findUserByNickname(nicname2);
                        if (clientShouldBeKicked != null) {
                            if (initiatorLvl < clientShouldBeKicked.getAccessLevel()) {
                                USERS_LOGGER.info("USER " + clientShouldBeKicked.getNickname() + " was disconnected by " + client.getNickname());
                                clientShouldBeKicked.sendMessage(msgOf(header(Library.USERS, Library.DISCONNECT), client.getNickname()));
                                clientShouldBeKicked.close();
                            } else {
                                USERS_LOGGER.error("FAILED TO DISCONNECT USER " + clientShouldBeKicked.getNickname() + " by  " + client.getNickname() + " cause permission of " + client.getNickname() + " is the same or lower than  permission of " + clientShouldBeKicked.getNickname());
                                client.sendMessage(msgOf(header(Library.USERS, Library.DISCONNECT, Library.DENIED), clientShouldBeKicked.getNickname()));
                            }
                        }
                        break;
                    case Library.BAN:
                        break;
                }
                break;
            case Library.PRODUCT_REQUEST:
                USERS_LOGGER.info("PRODUCT REQUEST...");
                ProductRequest request = Library.productRequestFromJson(receivedData.getData());
                List<Product> products = getProductsByFilter(request);
                sendProductList(products, client);
                break;
            case Library.IMAGE:
                USERS_LOGGER.info("IMAGE REQUEST...");
                int productID = Integer.parseInt(receivedData.getData());
                String imageID = SQLClient.getImageID(productID);
                if (imageID == null) {
                    SERVER_LOGGER.error("FAILED to get an image from DB for product with id: " + productID);
                    client.sendMessage(msgOf(header(Library.IMAGE, Library.EXCEPTION), String.valueOf(productID)));
                } else if (imageID.equals("NO_IMAGE")) {
                    SERVER_LOGGER.error("No image for product with id: " + productID);
                    client.sendMessage(msgOf(header(Library.IMAGE, Library.NO_IMAGE), String.valueOf(productID)));
                } else {
                    SERVER_LOGGER.info("Image for the product with id " + productID + " is on disk");
                    String imagePath = IMAGES_PATH + imageID;
                    sendImageToClient(client, productID, imagePath);
                }
        }
    }

    @Override
    public void onSocketThreadException(SocketThread thread, Exception e) {
        SERVER_LOGGER.error("ServerSocket exception: " + e.getMessage() + " " + e.getCause());
    }

    //ParserEvents
    @Override
    public int getCategoryID(String productName) {
        return SQLClient.getCategoryID(productName);
    }

    @Override
    public void insertProduct(String name, String url, int price, int categoryID, String name1, int volume, double strength) {
        SQLClient.insertProduct(name, url, price, categoryID, name1, volume, strength);
    }


    //Grabber Events
    @Override
    public void onGrabberReady() {
        SERVER_LOGGER.info("Grabber ready");
    }

    @Override
    public void onGrabberException(String message) {
        SERVER_LOGGER.error("Grabber exception: " + message);
    }

    @Override
    public void onGrabError() {
        SERVER_LOGGER.error("Grabber error");
    }

    @Override
    public void onGrabberSuccessfulEnd(int count) {
        SERVER_LOGGER.info("Parser successful end, total updates: " + count);
    }

    @Override
    public void insertCategory(String name) {
        SQLClient.insertCategory(name);
    }

    @Override
    public void insertStore(int region, String city, String address, String phone) {
        SQLClient.insertStore(region, city, address, phone);
    }

    //Updater Events
    @Override
    public void onUpdaterReady() {
        SERVER_LOGGER.info("Updater ready");
        sendMsgToModeratorsAndAdmins(msgOf(header(Library.UPDATER, Library.START)));
    }

    @Override
    public void onUpdaterException(int id, String url, Exception e) {
        SERVER_LOGGER.error("Updater exception: " + e.getMessage());
        sendMsgToModeratorsAndAdmins(msgOf(header(Library.UPDATER, Library.EXCEPTION), String.valueOf(id), url, e.getMessage()));
    }

    @Override
    public void onUpdateError() {
        SERVER_LOGGER.error("Updater error");
    }

    @Override
    public void onUpdateProductFailed(String url, int errorsCount) {
        SERVER_LOGGER.error("Update product failed");
        sendMsgToModeratorsAndAdmins(msgOf(header(Library.UPDATER, Library.FAILED), url, String.valueOf(errorsCount)));
    }

    @Override
    public void onUpdateDiffsFound(int count, String differences) {
        SERVER_LOGGER.info("Updater difference found: " + differences);
        sendMsgToModeratorsAndAdmins(msgOf(header(Library.UPDATER, Library.FOUND), String.valueOf(count), differences));
    }

    @Override
    public void onUpdaterCurrentProduct(int position, String name) {
        sendMsgToModeratorsAndAdmins(msgOf(header(Library.UPDATER, Library.CURRENT), String.valueOf(position), String.valueOf(updaterTotalProd), name));
    }

    @Override
    public void onUpdaterTotalProducts(int count) {
        SERVER_LOGGER.info("Update total products: " + count);
        updaterTotalProd = count;
        sendMsgToModeratorsAndAdmins(msgOf(header(Library.UPDATER, Library.PRODUCTS_TOTAL), String.valueOf(count)));
    }

    @Override
    public void onUpdateSuccessfulEnd(int checked, int updated, int errors) {
        SERVER_LOGGER.info("Updater successful end, total checked: " + checked + ", updates: " + updated + ", errors: " + errors);
        if (checked - 1 == updaterTotalProd) {
            SQLClient.updateUpdaterLastRun(0, updated, errors);
        } else {
            SQLClient.updateUpdaterLastRun(checked, updated, errors);
        }
        SQLClient.commit();

        lastUpdatedProductPosition = SQLClient.getLastUpdatedProductPosition();
        updaterLastRunDate = SQLClient.getProcessLastRun(UPDATER);
        sendMsgToModeratorsAndAdmins(msgOf(header(Library.UPDATER, Library.PROCESS_END), String.valueOf(checked), String.valueOf(updated), updaterLastRunDate.toString(), String.valueOf(lastUpdatedProductPosition)));
        sendMsgToModeratorsAndAdmins(msgOf(header(Library.UPDATER, Library.LAST_RUN), updaterLastRunDate.toString()));
        sendMsgToModeratorsAndAdmins(msgOf(header(Library.UPDATER, Library.LAST_POSITION), String.valueOf(lastUpdatedProductPosition)));
        updaterAutostartState = SQLClient.getProcessAutoStartState(UPDATER);

        if (updaterAutostartState) setUpdaterTimerTask();

    }

    @Override
    public void updateProductName(int id, String actualName) {
        SQLClient.updateProductName(id, actualName);
    }

    @Override
    public void updateProductGroupName(int id, String actualGroupName) {
        SQLClient.updateProductGroupName(id, actualGroupName);
    }

    @Override
    public void updateProductCategory(int id, int actualCategoryId) {
        SQLClient.updateProductCategory(id, actualCategoryId);
    }

    @Override
    public void updateProductPrice(int id, int actualPrice) {
        SQLClient.updateProductPrice(id, actualPrice);
    }

    @Override
    public void updateProductVolume(int id, int actualVolume) {
        SQLClient.updateProductVolume(id, actualVolume);
    }

    @Override
    public void updateProductStrength(int id, double actualStrength) {
        SQLClient.updateProductStrength(id, actualStrength);
    }

    @Override
    public void updateImageID(int id, String imageID) {
        SQLClient.updateImageID(id, imageID);
    }

    @Override
    public void updateProductRemains(int warehouseID, int productID, int remains) {
        SQLClient.updateProductRemains(warehouseID, productID, remains);
    }

    //Researcher Events
    @Override
    public void onResearcherReady() {
        SERVER_LOGGER.info("Researcher ready");
        sendMsgToModeratorsAndAdmins(msgOf(header(Library.RESEARCHER, Library.START)));
    }


    //TODO разобрать с этим
//    @Override
//    public void onUpdaterSQLException(Exception e) {
//        SERVER_LOGGER.error("Updater SQL exception: " + e.getMessage());
//        sendMsgToModeratorsAndAdmins(msgOf(header(Library.UPDATER, Library.EXCEPTION), e.getMessage()));
//    }
    @Override
    public void onResearchSuccessfulEnd(int count) {
        SERVER_LOGGER.info("Researcher successful end, total updates: " + count);
        SQLClient.updateResearcherLastRun(count);

        SQLClient.commit();
        researcherLastRunDate = SQLClient.getProcessLastRun(RESEARCHER);
        productsCount = SQLClient.getProductsCount();

        sendMsgToModeratorsAndAdmins(msgOf(header(Library.RESEARCHER, Library.PROCESS_END), String.valueOf(count)));
        sendMsgToModeratorsAndAdmins(msgOf(header(Library.RESEARCHER, Library.LAST_RUN), researcherLastRunDate.toString()));
        sendMsgToModeratorsAndAdmins(msgOf(header(Library.RESEARCHER, Library.LAST_RUN), researcherLastRunDate.toString()));
        researcherAutostartState = SQLClient.getProcessAutoStartState(RESEARCHER);

        if (researcherAutostartState) setResearcherTimerTask();
    }

    @Override
    public void onResearcherCurrentCategory(int categoriesCount, int currentCategory, String name) {
        sendMsgToModeratorsAndAdmins(msgOf(header(Library.RESEARCHER, Library.CURRENT_CATEGORY), String.valueOf(currentCategory), String.valueOf(categoriesCount), name));
    }

    @Override
    public void onResearcherCurrentGroup(int groupsCount, int currentGroup, String name) {
        sendMsgToModeratorsAndAdmins(msgOf(header(Library.RESEARCHER, Library.PRODUCTS_TOTAL), String.valueOf(groupsCount)));
        sendMsgToModeratorsAndAdmins(msgOf(header(Library.RESEARCHER, Library.CURRENT), String.valueOf(currentGroup), name));
    }

    @Override
    public void onResearcherFoundNewProduct(String name, int totalInserts) {
        sendMsgToModeratorsAndAdmins(msgOf(header(Library.RESEARCHER, Library.FOUND), name, String.valueOf(totalInserts)));
    }

    @Override
    public void onResearchError() {
        SERVER_LOGGER.error("Researcher error");
    }

    @Override
    public void onResearcherException(String message) {
        SERVER_LOGGER.error("Researcher exception: " + message);
    }

    @Override
    public boolean isProductAlreadyInDB(String url) {
        return SQLClient.isProductAlreadyInDB(url);
    }

    //Timer task's
    private void setUpdaterTimerTask() {
        SERVER_LOGGER.info("Updater TimerTask enabled");
        LocalDateTime startTime = calculateProcessRunDate(updaterLastRunDate, updaterAutostartTime, updaterDaysInterval);
        SERVER_LOGGER.info("Start time calculated for updater: " + startTime);
        updaterTimer.schedule(new UpdaterTimerTask(), Date.from(startTime.atZone(ZoneId.systemDefault()).toInstant()));
    }

    private void setResearcherTimerTask() {
        SERVER_LOGGER.info("Researcher TimerTask enabled");
        LocalDateTime startTime = calculateProcessRunDate(researcherLastRunDate, researcherAutostartTime, researcherDaysInterval);
        SERVER_LOGGER.info("Start time calculated for researcher: " + startTime);
        researcherTimer.schedule(new ResearcherTimerTask(), Date.from(startTime.atZone(ZoneId.systemDefault()).toInstant()));
    }

    private LocalDateTime calculateProcessRunDate(LocalDate lastCheckedTime, LocalTime runTime, int daysInterval) {
        SERVER_LOGGER.info("Start time calculating... ");
        int runHour = runTime.getHour();
        int runMinute = runTime.getMinute();
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime sheduleTime = LocalDateTime.of(lastCheckedTime.getYear(), lastCheckedTime.getMonthValue(), lastCheckedTime.getDayOfMonth() + daysInterval, runHour, runMinute);

        if (sheduleTime.isBefore(currentTime)) {
            int currYear = currentTime.getYear();
            int currMonth = currentTime.getMonthValue();
            int currDay = currentTime.getDayOfMonth();
            int currHour = currentTime.getHour();
            int currMinute = currentTime.getMinute();

            if (currHour == runHour) {
                if (currMinute >= runMinute) currDay++;
            } else if (currHour > runHour) currDay++;
            return LocalDateTime.of(currYear, currMonth, currDay, runHour, runMinute);
        }
        return sheduleTime;
    }

    private class ResearcherTimerTask extends TimerTask {
        @Override
        public void run() {
            SERVER_LOGGER.info("Researcher TimerTask executing...");
            startResearcher();
        }
    }

    private class UpdaterTimerTask extends TimerTask {
        @Override
        public void run() {
            SERVER_LOGGER.info("Updater TimerTask executing...");
            startUpdater(false);
        }
    }
}
