package main;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class Server implements ServerSocketThreadListener, SocketThreadListener {
    private ServerSocketThread server;
    private long serverStartAt;
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
            case Library.GET_SERVER_INFO:
                if (!client.isAuthorized()) {
                    client.sendMessage(Library.makeJsonString(Library.GET_SERVER_INFO, Library.DENIED));
                    clients.remove(client);
                    client.close();
                } else {
                    int accessLevel = client.getAccessLevel();
                    if (accessLevel == ClientThread.ADMIN || accessLevel == ClientThread.MODERATOR){
                        client.sendMessage(Library.makeJsonString(Library.GET_SERVER_INFO, Library.ACCEPTED, String.valueOf(accessLevel)));
                    } else {
                        client.sendMessage(Library.makeJsonString(Library.GET_SERVER_INFO, Library.DENIED));
                        clients.remove(client);
                    }
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

    private ClientThread findUserByNickname(String nickname) {
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if (!client.isAuthorized()) continue;
            if (client.getNickname().equals(nickname))
                return client;
        }
        return null;
    }
}
