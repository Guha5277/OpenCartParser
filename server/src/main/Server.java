package main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.ServerSocket;
import java.net.Socket;

public class Server implements ServerSocketThreadListener, SocketThreadListener{
    private ServerSocketThread server;
    //Logger LOG = LogManager.getLogger();

    public static void main(String[] args) {
        new Server();
    }

    private Server(){
        server = new ServerSocketThread(this, "server", 5277, 1000);
        server.start();
    }

    public boolean stop(){
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

    }

    @Override
    public void onServerAcceptTimeout(ServerSocketThread thread, ServerSocket server) {

    }

    @Override
    public void onSocketAccepted(ServerSocket server, Socket socket) {

    }

    @Override
    public void onServerException(ServerSocketThread thread, Exception e) {

    }

    @Override
    public void onThreadStop(ServerSocketThread thread) {

    }

    //SocketEvents
    @Override
    public void onSocketThreadStart(SocketThread thread, Socket socket) {

    }

    @Override
    public void onSocketThreadStop(SocketThread thread) {

    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, String msg) {

    }

    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {

    }

    @Override
    public void onSocketThreadException(SocketThread thread, Exception e) {

    }
}
