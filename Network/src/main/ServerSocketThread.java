package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ServerSocketThread extends Thread {

    private final int port;
    private final int timeout;
    private final ServerSocketThreadListener listener;

    public ServerSocketThread(ServerSocketThreadListener listener, String name, int port, int timeout) {
        super(name);
        this.port = port;
        this.timeout = timeout;
        this.listener = listener;
        start();
    }

    @Override
    public void run() {
        listener.onThreadStart(this);
        try (ServerSocket server = new ServerSocket(port)) {
            server.setSoTimeout(timeout);
            listener.onServerStart(this, server);
            Socket socket;
            while (!isInterrupted()) {
                try {
                    socket = server.accept();
                } catch (SocketTimeoutException e) {
                    listener.onServerAcceptTimeout(this, server);
                    continue;
                }
                listener.onSocketAccepted(server, socket);
            }
        } catch (IOException e) {
            listener.onServerException(this, e);
        } finally {
            listener.onThreadStop(this);
        }
    }
}
