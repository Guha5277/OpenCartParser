package main;

import main.product.Product;
import main.product.Warehouse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Vector;

public class Server implements ServerSocketThreadListener, SocketThreadListener, ParserEvents {
    private long serverStartAt;
    private int productsCount;
    private int warehousesCount;
    private int updaterTotalProd;
    private String updaterLastRunDate;
    private String researcherLastRunDate;
    private int lastUpdatedProductPosition;
    private Thread updaterThread;
    private Thread researcherThread;
    private Updater updater;
    private List<Warehouse> warehouses;
    //private Grabber grabber;
    private Researcher researcher;
    private Vector<SocketThread> clients = new Vector<>();
    private static final Logger SERVER_LOGGER = LogManager.getLogger("ServerLogger");
    private static final Logger USERS_LOGGER = LogManager.getLogger("UsersLogger");
    private static final Logger PARSER_LOGGER = LogManager.getLogger("ParserLogger");

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
        SQLClient.connect();
        productsCount = SQLClient.getProductsCount();
        warehousesCount = SQLClient.getWarehousesCount();
        updaterLastRunDate = SQLClient.getUpdaterLastRun();
        researcherLastRunDate = SQLClient.getResearcherLastRun();
        lastUpdatedProductPosition = SQLClient.getLastUpdatedProductPosition();
        String address = "";
        String name = "";
        try {
            address = InetAddress.getLocalHost().getHostAddress();
            name = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        SERVER_LOGGER.info("Server Started " + name + " " + address);
    }

    @Override
    public void onServerAcceptTimeout(ServerSocketThread thread, ServerSocket server) {

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
                    USERS_LOGGER.warn("AUTH FAILED cause MESSAGE FORMAT ERROR header length: " + header.length);
                    return;
                }
                authorizeClient(client, receivedData.getData());
                break;
            case Library.SERVER_INFO:
                if (!client.isAuthorized()) {
                    client.sendMessage(Library.makeJsonString(Library.SERVER_INFO, Library.DENIED));
                    clients.remove(client);
                    client.close();
                } else {
                    int accessLevel = client.getAccessLevel();
                    if (accessLevel == ClientThread.ADMIN || accessLevel == ClientThread.MODERATOR) {
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
                            client.sendMessage(Library.makeJsonString(Library.UPDATER, Library.LAST_RUN, updaterLastRunDate));
                        }
                        if (researcherLastRunDate != null) {
                            client.sendMessage(Library.makeJsonString(Library.RESEARCHER, Library.LAST_RUN, researcherLastRunDate));
                        }
                    } else {
                        client.sendMessage(Library.makeJsonString(Library.SERVER_INFO, Library.DENIED));
                        clients.remove(client);
                    }
                }
                break;
            case Library.UPDATER:
                if (client.getAccessLevel() > 2) {
                    client.sendMessage(Library.makeJsonString(Library.UPDATER, Library.DENIED));
                    clients.remove(client);
                    client.close();
                    return;
                }
                switch (header[1]) {
                    case Library.START:
                        startUpdater(Boolean.valueOf(receivedData.getData()));
                        break;
                    case Library.STOP:
                        stopUpdater();
                        break;
                }
                break;

            case Library.RESEARCHER:
                if (client.getAccessLevel() > 2) {
                    client.sendMessage(Library.makeJsonString(Library.RESEARCHER, Library.DENIED));
                    clients.remove(client);
                    client.close();
                    return;
                }
                switch (header[1]) {
                    case Library.START:
                        startResearcher();
                        break;
                    case Library.STOP:
                        stopResearcher();
                        break;
                }
                break;
            case Library.USERS:
                switch (header[1]) {
                    case Library.DISCONNECT:
                        String nickname = receivedData.getData();
                        int initiatorLvl = client.getAccessLevel();
                        ClientThread clientShouldBeKicked = findUserByNickname(nickname);
                        if (clientShouldBeKicked != null) {
                            if (initiatorLvl < clientShouldBeKicked.getAccessLevel()) {
                                clientShouldBeKicked.sendMessage(Library.makeJsonString(Library.USERS, Library.DISCONNECT, client.getNickname()));
                                clientShouldBeKicked.close();
                            } else {
                                client.sendMessage(Library.makeJsonString(Library.USERS, Library.DISCONNECT, Library.DENIED, clientShouldBeKicked.getNickname()));
                            }
                        }
                        break;
                    case Library.BAN:
                        break;
                }
                break;
            case Library.PRODUCT_REQUEST:
                ProductRequest request = Library.productRequestFromJson(receivedData.getData());
                List<Product> products = getProductsByFilter(request);
                sendProductList(products);
                break;
        }
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

    private void sendProductList(List<Product> list){
        if (list == null){
            System.out.println("Product list is null!");
            sendMsgToModersAndAdmins(Library.makeJsonString(Library.PRODUCT_REQUEST, Library.EMPTY));
            return;
        }

        sendMsgToModersAndAdmins(Library.makeJsonString(Library.PRODUCT_LIST_START));

        for (Product p : list){
            sendMsgToModersAndAdmins(Library.productToJson(p));
        }
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.PRODUCT_LIST_END));
    }

    private void sendWarehousesList(SocketThread thread) {
        if (warehouses == null) warehouses = SQLClient.getAllWarehouses();
        for (Warehouse w : warehouses) {
            thread.sendMessage(Library.warehouseToJson(w));
        }
        thread.sendMessage(Library.makeJsonString(Library.WAREHOUSE_LIST_END));
    }

    @Override
    public void onSocketThreadException(SocketThread thread, Exception e) {
        //thread.close();
        System.out.println("ServerSocket exception: " + e.getMessage() + " " + e.getCause());
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
    }

    private void stopUpdater() {
        if (updaterThread == null || !updaterThread.isAlive()) return;
        updaterThread.interrupt();
        updater.stop();
        updaterThread = null;
        updater = null;
    }

    private void startResearcher() {
        if (researcherThread != null && updaterThread.isAlive()) return;
        researcher = new Researcher("https://ilfumoshop.ru/zhidkost-dlya-zapravki-vejporov", this);
        researcherThread = new Thread(researcher);
        researcherThread.start();
    }

    private void stopResearcher() {
        if (researcherThread == null || !researcherThread.isAlive()) return;
        researcherThread.interrupt();
        researcher.stop();
        researcherThread = null;
        researcher = null;
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

    }

    @Override
    public void onUpdaterReady() {
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.UPDATER, Library.START));
    }

    @Override
    public void onResearcherReady() {
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.RESEARCHER, Library.START));
    }

    @Override
    public void onParserException(Exception e) {

    }

    @Override
    public void onUpdaterException(int id, String url, Exception e) {
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.UPDATER, Library.EXCEPTION, String.valueOf(id), url, e.getMessage()));
    }

    @Override
    public void onUpdaterSQLException(Exception e) {
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.UPDATER, Library.EXCEPTION, e.getMessage()));
    }

    @Override
    public void onUpdateProductFailed(String url, int errorsCount) {
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.UPDATER, Library.FAILED, url, String.valueOf(errorsCount)));
    }

    @Override
    public void onUpdateDiffsFound(int count, String differences) {
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.UPDATER, Library.FOUND, String.valueOf(count), differences));
    }

    @Override
    public void onUpdaterCurrentProduct(int position, String name) {
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.UPDATER, Library.CURRENT, String.valueOf(position), String.valueOf(updaterTotalProd), name));
    }

    @Override
    public void onUpdaterTotalProducts(int count) {
        updaterTotalProd = count;
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.UPDATER, Library.PRODUCTS_TOTAL, String.valueOf(count)));
    }

    @Override
    public void onGrabError() {

    }

    @Override
    public void onUpdateError() {

    }


    @Override
    public void onParseSuccessfulEnd(int count) {

    }

    @Override
    public void onUpdateSuccessfulEnd(int checked, int updated, int errors) {
        if (checked - 1 == updaterTotalProd) {
            SQLClient.updateUpdaterLastRun(0, updated, errors);
        } else {
            SQLClient.updateUpdaterLastRun(checked, updated, errors);
        }
        SQLClient.commit();

        lastUpdatedProductPosition = SQLClient.getLastUpdatedProductPosition();
        updaterLastRunDate = SQLClient.getUpdaterLastRun();

        sendMsgToModersAndAdmins(Library.makeJsonString(Library.UPDATER, Library.PROCESS_END, String.valueOf(checked), String.valueOf(updated)));
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.UPDATER, Library.LAST_RUN, updaterLastRunDate));
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.UPDATER, Library.LAST_POSITION, String.valueOf(lastUpdatedProductPosition)));
    }

    @Override
    public void onResearchSuccessfulEnd(int count) {
        SQLClient.updateResearcherLastRun(count);

        SQLClient.commit();
        researcherLastRunDate = SQLClient.getResearcherLastRun();
        productsCount = SQLClient.getProductsCount();

        sendMsgToModersAndAdmins(Library.makeJsonString(Library.RESEARCHER, Library.PROCESS_END, String.valueOf(count)));
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.RESEARCHER, Library.LAST_RUN, researcherLastRunDate));
        sendMsgToModersAndAdmins(Library.makeJsonString(Library.RESEARCHER, Library.LAST_RUN, researcherLastRunDate));
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
        /*TODO*/
    }
}
