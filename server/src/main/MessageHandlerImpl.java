package main;

public interface MessageHandlerImpl {
    void handleMessage(SocketThread socketThread, String msg);
}
