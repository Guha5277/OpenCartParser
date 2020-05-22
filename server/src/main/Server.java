package main;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class Server implements ServerSocketThreadListener, SocketThreadListener, ParserEvents {
    private ServerSocketThread server;
    private long serverStartAt;
    private int productsCount;
    private int warehousesCount;
    private int updaterTotalProd;
    private Thread updaterThread;
    private Updater updater;
    private Grabber grabber;
    private Researcher researcher;
    private Vector<SocketThread> clients = new Vector<>();

    public static void main(String[] args) {
        new Server();
    }

    private Server() {
        server = new ServerSocketThread(this, "server", 5277, 200);
        serverStartAt = System.currentTimeMillis();
    }

    public boolean stop() {
        if (server == null || !server.isAlive()) return false;
        server.interrupt();
        return true;
    }

    //Server Events
    @Override
    public void onThreadStart(ServerSocketThread thread) {

    }

    @Override
    public void onServerStart(ServerSocketThread thread, ServerSocket server) {
        System.out.println("Server started: " + server.getLocalSocketAddress());
        SQLClient.connect();
        productsCount = SQLClient.getProductsCount();
        warehousesCount = SQLClient.getWarehousesCount();
    }

    @Override
    public void onServerAcceptTimeout(ServerSocketThread thread, ServerSocket server) {

    }

    @Override
    public void onSocketAccepted(ServerSocket server, Socket socket) {
        System.out.println("Socket accepted");
        new ClientThread(this, "client", socket);
    }

    @Override
    public void onServerException(ServerSocketThread thread, Exception e) {
        System.out.println("Server exceprion");
    }

    @Override
    public void onThreadStop(ServerSocketThread thread) {
        System.out.println("Server stop");
        SQLClient.disconnect();
    }

    //SocketEvents
    @Override
    public void onSocketThreadStart(SocketThread thread, Socket socket) {
        System.out.println("ServerSocket thread start");
    }

    @Override
    public void onSocketThreadStop(SocketThread thread) {
        System.out.println("ServerSocket thread stop");
        clients.remove(thread);
    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, String msg) {
        System.out.println("ServerSocket thread received msg");
        ClientThread client = (ClientThread) thread;
        DataProtocol receivedData = Library.jsonToObject(msg);
        byte[] header = receivedData.getHeader();
        switch (header[0]) {
            case Library.AUTH:
                if (header.length < 2 || header[1] != Library.REQUEST) {
                    client.msgFormatError(Library.makeJsonString(Library.MESSAGE_FORMAT_ERROR));
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
                    if (accessLevel == ClientThread.ADMIN || accessLevel == ClientThread.MODERATOR){
                        client.sendMessage(Library.makeJsonString(Library.SERVER_INFO, Library.ACCEPTED, String.valueOf(accessLevel)));
                        client.sendMessage(Library.makeJsonString(Library.START_TIME, String.valueOf(serverStartAt)));
                        client.sendMessage(Library.makeJsonString(Library.PRODUCTS_COUNT, String.valueOf(productsCount)));
                        client.sendMessage(Library.makeJsonString(Library.WAREHOUSES_COUNT, String.valueOf(warehousesCount)));
                        client.sendMessage(Library.makeJsonString(Library.ACTIVE_USERS, String.valueOf(clients.size())));
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
                boolean result;
                if (header[1] == Library.START){
                    startUpdater();
//                    result = startUpdater();
//                    client.sendMessage(Library.makeJsonString(Library.UPDATER, Library.START, String.valueOf(result)));
                } else if(header[1] == Library.STOP){
                    stopUpdater();
//                    result = stopUpdater();
//                    client.sendMessage(Library.makeJsonString(Library.UPDATER, Library.STOP, String.valueOf(result)));
                }
        }
    }

    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {
        System.out.println("ServerSocket Ready");
        clients.add(thread);
    }

    @Override
    public void onSocketThreadException(SocketThread thread, Exception e) {
        thread.close();
        System.out.println("ServerSocket exception: " + e.getMessage() + " " + e.getCause());
    }

    private void authorizeClient(ClientThread clientThread, String userData) {
        String[] data = userData.split(Library.DELIMITER);
        String login = data[0];
        String password = data[1];

        String nickname = SQLClient.getNickname(login, password);
        if (nickname == null) {
            clientThread.authFailed(Library.makeJsonString(Library.AUTH, Library.DENIED));
        } else {
            ClientThread client = findUserByNickname(nickname);
            if (client == null) {
                clientThread.authAccept(nickname);
                clientThread.sendMessage(Library.makeJsonString(Library.AUTH, Library.ACCEPTED, nickname));
                int userAccessLevel = SQLClient.getUserRole(nickname);
                clientThread.setAccessLevel(userAccessLevel);
            }
        }
    }

    private void startUpdater(){
        if (updaterThread != null && updaterThread.isAlive()) return;
        updater = new Updater(this);
        updaterThread = new Thread(updater);
        updaterThread.start();
    }

    private void stopUpdater(){
        if (updaterThread == null || !updaterThread.isAlive()) return;
        updaterThread.interrupt();
        updater.stop();
        updaterThread = null;
        updater = null;
    }

    private ClientThread findUserByNickname(String nickname) {
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if (!client.isAuthorized()) continue;
            if (client.getNickname().equals(nickname))
                return client;
        }
        return null;
    }

    private void sendMessageToAllClients(int accessLvl, String msg){
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if (!client.isAuthorized() || client.getAccessLevel() > accessLvl) continue;
            client.sendMessage(msg);
        }
    }

    //Services events
    @Override
    public void onGrabberReady() {

    }

    @Override
    public void onUpdaterReady() {
        if (clients.size() > 0){
            sendMessageToAllClients(Library.MODERATOR, Library.makeJsonString(Library.UPDATER, Library.START));
        }
    }

    @Override
    public void onResearcherReady() {

    }

    @Override
    public void onParserException(Exception e) {

    }

    @Override
    public void onUpdateProductFailed(String url, int errorsCount) {
        if (clients.size() > 0){
            sendMessageToAllClients(Library.MODERATOR, Library.makeJsonString(Library.UPDATER, Library.FAILED, url, String.valueOf(errorsCount)));
        }
    }

    @Override
    public void onUpdateDiffsFound(int count) {
        if (clients.size() > 0){
            sendMessageToAllClients(Library.MODERATOR, Library.makeJsonString(Library.UPDATER, Library.FOUND, String.valueOf(count)));
        }
    }

    @Override
    public void onUpdaterCurrentProduct(int position, String name) {
        if (clients.size() > 0){
            sendMessageToAllClients(Library.MODERATOR, Library.makeJsonString(Library.UPDATER, Library.CURRENT, String.valueOf(position), String.valueOf(updaterTotalProd), name));
        }
    }

    @Override
    public void onUpdaterTotalProducts(int count) {
        updaterTotalProd = count;
        if (clients.size() > 0){
            sendMessageToAllClients(Library.MODERATOR, Library.makeJsonString(Library.UPDATER, Library.PRODUCTS_TOTAL, String.valueOf(count)));
        }
    }

    @Override
    public void onGrabError() {

    }

    @Override
    public void onUpdateError() {

    }

    @Override
    public void onResearchError() {

    }

    @Override
    public void onParseSuccessfulEnd(int count) {

    }

    @Override
    public void onUpdateSuccessfulEnd(int checked, int updated) {
        if (clients.size() > 0){
            sendMessageToAllClients(Library.MODERATOR, Library.makeJsonString(Library.UPDATER, Library.PROCESS_END, String.valueOf(checked), String.valueOf(updated)));
        }
        SQLClient.commit();
    }

    @Override
    public void onResearchSuccessfulEnd(int count) {

    }
}
