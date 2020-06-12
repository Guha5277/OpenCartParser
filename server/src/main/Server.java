package main;

import main.product.Product;
import main.product.Warehouse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

public class Server implements ServerSocketThreadListener, SocketThreadListener, ParserEvents {
    private long serverStartAt;
    private int productsCount;
    private int warehousesCount;
    private int updaterTotalProd;
    private final String UPDATER = "updater";
    private final String RESEARCHER = "researcher";
    private LocalDate updaterLastRunDate;
    private LocalDate researcherLastRunDate;
    private int lastUpdatedProductPosition;
    private boolean updaterAutostartEnabled;
    private boolean researcherAutostartEnabled;
    private LocalTime updaterAutostartTime;
    private LocalTime researcherAutostartTime;
    private int updaterDaysInterval = 1;
    private int researcherDaysInterval = 1;
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
    //private static final Logger PARSER_LOGGER = LogManager.getLogger("ParserLogger");

    public static void main(String[] args) {
        new Server();
    }

    private Server() {
        new ServerSocketThread(this, "server", 5277, 200);
        serverStartAt = System.currentTimeMillis();
    }

    //Server Events
    @Override
    public void onThreadStart(ServerSocketThread thread) {
        SERVER_LOGGER.info("Server Thread Started");
    }

    @Override
    public void onServerStart(ServerSocketThread thread, ServerSocket server) {
        SERVER_LOGGER.info("Server initializing...");
        SQLClient.connect();
        productsCount = SQLClient.getProductsCount();
        warehousesCount = SQLClient.getWarehousesCount();
        updaterLastRunDate = SQLClient.getProcessLastRun(UPDATER);
        updaterAutostartTime = SQLClient.getProcessLaunchTime(UPDATER);
        updaterAutostartEnabled = SQLClient.getProcessAutoStartState(UPDATER);

        researcherLastRunDate = SQLClient.getProcessLastRun(RESEARCHER);
        researcherAutostartTime = SQLClient.getProcessLaunchTime(RESEARCHER);
        researcherAutostartEnabled = SQLClient.getProcessAutoStartState(RESEARCHER);
        lastUpdatedProductPosition = SQLClient.getLastUpdatedProductPosition();

        if (updaterAutostartEnabled){
            runUpdaterTimerTask();
        }

        if (researcherAutostartEnabled){
            runResearcherTimerTask();
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
        SERVER_LOGGER.info("Client SocketThread started "  + socket.getInetAddress());
    }

    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {
        SERVER_LOGGER.info("Client SocketThread ready and added to list"  + socket.getInetAddress());
        clients.add(thread);
    }

    @Override
    public void onSocketThreadStop(SocketThread thread) {
        ClientThread clientThread = (ClientThread) thread;
        USERS_LOGGER.info("Client disconnected: " + clientThread.getNickname());
        clients.remove(thread);
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.USERS, Library.COUNT, String.valueOf(clients.size())));
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.USERS, Library.LIST, getListOfClients()));
    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, String msg) {
        SERVER_LOGGER.info("Received message from client ("+ msg.length() +")");
        ClientThread client = (ClientThread) thread;
        DataProtocol receivedData = Library.jsonToObject(msg);
        byte[] header = receivedData.getHeader();
        switch (header[0]) {
            case Library.AUTH:
                if (header.length < 2 || header[1] != Library.REQUEST) {
                    client.msgFormatError(Library.makeJsonString(Library.MESSAGE_FORMAT_ERROR));
                    USERS_LOGGER.error("AUTH FAILED cause MESSAGE FORMAT ERROR header length: " + header.length);
                    return;
                }
                authorizeClient(client, receivedData.getData());
                break;
            case Library.SERVER_INFO:
                if (!client.isAuthorized()) {
                    USERS_LOGGER.error("GET SERVER_INFO FAILED cause client is not authorized!");
                    client.sendMessage(Library.makeJsonString(Library.SERVER_INFO, Library.DENIED));
                    clients.remove(client);
                    client.close();
                } else {
                    int accessLevel = client.getAccessLevel();
                    if (accessLevel == ClientThread.ADMIN || accessLevel == ClientThread.MODERATOR) {
                        USERS_LOGGER.info("GET SERVER_INFO ACCEPTED for " + client.getNickname());
                        client.sendMessage(Library.makeJsonString(Library.SERVER_INFO, Library.ACCEPTED, String.valueOf(accessLevel)));
                        client.sendMessage(Library.makeJsonString(Library.START_TIME, String.valueOf(serverStartAt)));
                        client.sendMessage(Library.makeJsonString(Library.PRODUCTS_COUNT, String.valueOf(productsCount)));
                        client.sendMessage(Library.makeJsonString(Library.WAREHOUSES_COUNT, String.valueOf(warehousesCount)));
                        sendMsgToModersAndAdmins(Library.makeJsonString(Library.USERS, Library.COUNT, String.valueOf(clients.size())));
                        sendMsgToModersAndAdmins(Library.makeJsonString(Library.USERS, Library.LIST, getListOfClients()));
                        sendMsgToModersAndAdmins(Library.makeJsonString(Library.UPDATER, Library.LAST_POSITION, String.valueOf(lastUpdatedProductPosition)));

                        //send warehouses list to client
                        sendWarehousesList(thread);

                        if (updaterLastRunDate != null) {
                            client.sendMessage(Library.makeJsonString(Library.UPDATER, Library.LAST_RUN, updaterLastRunDate.toString()));
                        }
                        if (researcherLastRunDate != null) {
                            client.sendMessage(Library.makeJsonString(Library.RESEARCHER, Library.LAST_RUN, researcherLastRunDate.toString()));
                        }
                    } else {
                        USERS_LOGGER.error("GET SERVER_INFO FAILED cause client access level is lower than required: " + accessLevel);
                        client.sendMessage(Library.makeJsonString(Library.SERVER_INFO, Library.DENIED));
                    }
                }
                break;
            case Library.UPDATER:
                USERS_LOGGER.info("UPDATER...");
                int accessLevel = client.getAccessLevel();
                String nickname = client.getNickname();
                if (accessLevel > 2) {
                    USERS_LOGGER.error("ACCESS TO UPDATER DENIED cause client access level is lower than required: " + accessLevel);
                    client.sendMessage(Library.makeJsonString(Library.UPDATER, Library.DENIED));
                    return;
                }
                switch (header[1]) {
                    case Library.START:
                        USERS_LOGGER.info("start UPDATER request by " + nickname );
                        startUpdater(Boolean.valueOf(receivedData.getData()));
                        break;
                    case Library.STOP:
                        USERS_LOGGER.info("stop UPDATER request by " + nickname );
                        stopUpdater();
                        break;
                }
                break;

            case Library.RESEARCHER:
                USERS_LOGGER.info("RESEARCHER...");
                if (client.getAccessLevel() > 2) {
                    USERS_LOGGER.error("ACCESS TO RESEARCHER DENIED cause client access level is lower than required: " + client.getAccessLevel());
                    client.sendMessage(Library.makeJsonString(Library.RESEARCHER, Library.DENIED));
                    clients.remove(client);
                    client.close();
                    return;
                }
                switch (header[1]) {
                    case Library.START:
                        USERS_LOGGER.info("start RESEARCHER request by " + client.getNickname() );
                        startResearcher();
                        break;
                    case Library.STOP:
                        USERS_LOGGER.info("stop RESEARCHER request by " + client.getNickname() );
                        stopResearcher();
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
                                clientShouldBeKicked.sendMessage(Library.makeJsonString(Library.USERS, Library.DISCONNECT, client.getNickname()));
                                clientShouldBeKicked.close();
                            } else {
                                USERS_LOGGER.error("FAILED TO DISCONNECT USER " + clientShouldBeKicked.getNickname() + " by  " + client.getNickname() + " cause permission of " + client.getNickname() + " is the same or lower than  permission of " + clientShouldBeKicked.getNickname());
                                client.sendMessage(Library.makeJsonString(Library.USERS, Library.DISCONNECT, Library.DENIED, clientShouldBeKicked.getNickname()));
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
        }
    }

    @Override
    public void onSocketThreadException(SocketThread thread, Exception e) {
        SERVER_LOGGER.error("ServerSocket exception: " + e.getMessage() + " " + e.getCause());
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

            if (regionID == -1 && storeID == -1){
                stockFilter = " AND product_remains.remains > 0";
            } else if (storeID != -1) {
                stockFilter = " AND product_remains.remains > 0 AND warehouse.id=" + storeID;
                singleStore = true;
            } else {
                stockFilter = " AND product_remains.remains > 0 AND warehouse.region=" + regionID;
            }

            if (singleStore){
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

    private void sendProductList(List<Product> list, ClientThread client){
        int listSize = list.size();
        if (listSize == 0){
            SERVER_LOGGER.info("No result found for PRODUCT_REQUEST");
            sendMsgToModersAndAdmins(Library.makeJsonString(Library.PRODUCT_REQUEST, Library.EMPTY));
            return;
        }

        SERVER_LOGGER.info(listSize + " products found for PRODUCT_REQUEST");

        client.sendMessage(Library.makeJsonString(Library.PRODUCT_LIST_START));

        for (Product p : list){
            client.sendMessage(Library.productToJson(p));
        }
        client.sendMessage(Library.makeJsonString(Library.PRODUCT_LIST_END));

        SERVER_LOGGER.info(listSize + " products sent to client successful");
    }

    private void sendWarehousesList(SocketThread thread) {
        if (warehouses == null) warehouses = SQLClient.getAllWarehouses();
        for (Warehouse w : warehouses) {
            thread.sendMessage(Library.warehouseToJson(w));
        }
        thread.sendMessage(Library.makeJsonString(Library.WAREHOUSE_LIST_END));
    }

    private void authorizeClient(ClientThread clientThread, String userData) {
        String[] data = userData.split(Library.DELIMITER);
        String login = data[0];
        String password = data[1];
        USERS_LOGGER.info("AUTH REQUEST for " + login);

        String nickname = SQLClient.getNickname(login, password);
        if (nickname == null) {
            USERS_LOGGER.error("AUTH FAILED for " + login + "! Invalid login/password");
            clientThread.authFailed(Library.makeJsonString(Library.AUTH, Library.DENIED));
            clientThread.close();
        } else {
            ClientThread client = findUserByNickname(nickname);
            if (client == null) {
                clientThread.authAccept(nickname);
                clientThread.sendMessage(Library.makeJsonString(Library.AUTH, Library.ACCEPTED, nickname));
                int userAccessLevel = SQLClient.getUserRole(nickname);
                clientThread.setAccessLevel(userAccessLevel);
                USERS_LOGGER.info("AUTH SUCCESS for " + login + " with nickname: + " + nickname);
            } else {
                client.sendMessage(Library.makeJsonString(Library.AUTH, Library.MULTIPLY_SESSION, client.getNickname()));
                clientThread.close();
                USERS_LOGGER.error("AUTH FAILED for " + login + "! Client connected already");
            }
        }
    }

    private void startUpdater(boolean continueUpdate) {
        if (updaterThread != null && updaterThread.isAlive()) return;
        if (continueUpdate && lastUpdatedProductPosition < productsCount) {
            updater = new Updater(this, lastUpdatedProductPosition);
        } else {
            updater = new Updater(this, 0);
        }
        updaterThread = new Thread(updater);
        updaterThread.start();
        SERVER_LOGGER.info("Updater started");
    }

    private void stopUpdater() {
        if (updaterThread == null || !updaterThread.isAlive()) return;
        updaterThread.interrupt();
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

    private void sendMsgToModersAndAdmins(String msg) {
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

    //Services events
    @Override
    public void onGrabberReady() {
        SERVER_LOGGER.info("Grabber ready");
    }

    @Override
    public void onUpdaterReady() {
        SERVER_LOGGER.info("Updater ready");
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.UPDATER, Library.START));
    }

    @Override
    public void onResearcherReady() {
        SERVER_LOGGER.info("Researcher ready");
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.RESEARCHER, Library.START));
    }

    @Override
    public void onParserException(Exception e) {
        SERVER_LOGGER.error("Parser exception: " + e.getMessage());
    }

    @Override
    public void onUpdaterException(int id, String url, Exception e) {
        SERVER_LOGGER.error("Updater exception: " + e.getMessage());
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.UPDATER, Library.EXCEPTION, String.valueOf(id), url, e.getMessage()));
    }

    @Override
    public void onUpdaterSQLException(Exception e) {
        SERVER_LOGGER.error("Updater SQL exception: " + e.getMessage());
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.UPDATER, Library.EXCEPTION, e.getMessage()));
    }

    @Override
    public void onUpdateProductFailed(String url, int errorsCount) {
        SERVER_LOGGER.error("Update product failed");
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.UPDATER, Library.FAILED, url, String.valueOf(errorsCount)));
    }

    @Override
    public void onUpdateDiffsFound(int count, String differences) {
        SERVER_LOGGER.info("Updater difference found: " + differences);
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.UPDATER, Library.FOUND, String.valueOf(count), differences));
    }

    @Override
    public void onUpdaterCurrentProduct(int position, String name) {
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.UPDATER, Library.CURRENT, String.valueOf(position), String.valueOf(updaterTotalProd), name));
    }

    @Override
    public void onUpdaterTotalProducts(int count) {
        SERVER_LOGGER.info("Update total products: " + count);
        updaterTotalProd = count;
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.UPDATER, Library.PRODUCTS_TOTAL, String.valueOf(count)));
    }

    @Override
    public void onGrabError() {
        SERVER_LOGGER.error("Grabber error");
    }

    @Override
    public void onUpdateError() {
        SERVER_LOGGER.error("Updater error");
    }

    @Override
    public void onParseSuccessfulEnd(int count) {
        SERVER_LOGGER.info("Parser successful end, total updates: " + count);
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
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.UPDATER, Library.PROCESS_END, String.valueOf(checked), String.valueOf(updated)));
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.UPDATER, Library.LAST_RUN, updaterLastRunDate.toString()));
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.UPDATER, Library.LAST_POSITION, String.valueOf(lastUpdatedProductPosition)));
        updaterAutostartEnabled = SQLClient.getProcessAutoStartState(UPDATER);

        if (updaterAutostartEnabled) runUpdaterTimerTask();

    }

    @Override
    public void onResearchSuccessfulEnd(int count) {
        SERVER_LOGGER.info("Researcher successful end, total updates: " + count);
        SQLClient.updateResearcherLastRun(count);

        SQLClient.commit();
        researcherLastRunDate = SQLClient.getProcessLastRun(RESEARCHER);
        productsCount = SQLClient.getProductsCount();

        sendMsgToModersAndAdmins(Library.makeJsonString(Library.RESEARCHER, Library.PROCESS_END, String.valueOf(count)));
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.RESEARCHER, Library.LAST_RUN, researcherLastRunDate.toString()));
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.RESEARCHER, Library.LAST_RUN, researcherLastRunDate.toString()));
        researcherAutostartEnabled = SQLClient.getProcessAutoStartState(RESEARCHER);

        if (researcherAutostartEnabled) runResearcherTimerTask();
    }

    @Override
    public void onResearcherCurrentCategory(int categoriesCount, int currentCategory, String name) {
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.RESEARCHER, Library.CURRENT_CATEGORY, String.valueOf(currentCategory), String.valueOf(categoriesCount), name));
    }

    @Override
    public void onResearcherCurrentGroup(int groupsCount, int currentGroup, String name) {
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.RESEARCHER, Library.PRODUCTS_TOTAL, String.valueOf(groupsCount)));
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.RESEARCHER, Library.CURRENT, String.valueOf(currentGroup), name));
    }

    @Override
    public void onResearcherFoundNewProduct(String name, int totalInserts) {
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.RESEARCHER, Library.FOUND, name, String.valueOf(totalInserts)));
    }

    @Override
    public void onResearchError() {
        SERVER_LOGGER.error("Researcher error");
    }

    private void runUpdaterTimerTask(){
        updaterTimer.schedule(new UpdaterTimerTask(), Date.from(calculateProcessRunDate(updaterLastRunDate, updaterAutostartTime, updaterDaysInterval).atZone(ZoneId.systemDefault()).toInstant()));
    }

    private void runResearcherTimerTask(){
         researcherTimer.schedule(new ResearcherTimerTask(), Date.from(calculateProcessRunDate(researcherLastRunDate, researcherAutostartTime, researcherDaysInterval).atZone(ZoneId.systemDefault()).toInstant()));
    }

    private LocalDateTime calculateProcessRunDate(LocalDate lastCheckedTime, LocalTime runTime, int daysInterval) {
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
            startResearcher();
            //if (researcherAutostartEnabled) runResearcherTimerTask();
        }
    }

    private class UpdaterTimerTask extends TimerTask {
        @Override
        public void run() {
            startUpdater(false);
            //if (updaterAutostartEnabled) runUpdaterTimerTask();
        }
    }
}
