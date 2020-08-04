package main;

import java.net.ServerSocket;
import java.net.Socket;

public interface ServerSocketThreadListener {
    void onThreadStart(ServerSocketThread thread);
    void onServerStart(ServerSocketThread thread, ServerSocket server);
    default void onServerAcceptTimeout(ServerSocketThread thread, ServerSocket server) {}
    void onSocketAccepted(ServerSocket server, Socket socket);
    void onServerException(ServerSocketThread thread, Exception e);
    void onThreadStop(ServerSocketThread thread);
}
