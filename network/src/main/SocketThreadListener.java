package main;

import java.net.Socket;

public interface SocketThreadListener {
    void onSocketThreadStart(SocketThread thread, Socket socket);
    void onSocketThreadStop(SocketThread thread);

    void onReceiveString(SocketThread thread, Socket socket, String msg);
    void onSocketReady(SocketThread thread, Socket socket);

    void onSocketThreadException(SocketThread thread, Exception e);

}
