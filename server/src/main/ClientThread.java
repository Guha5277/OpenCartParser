package main;

import java.net.Socket;

public class ClientThread extends SocketThread {
    public static final int ADMIN = 1;
    public static final int MODERATOR = 2;
    public static final int USER = 3;
    public static final int GHOST = 4;

    private String nickname;
    private boolean isAuthorized;
    private int accessLevel = GHOST;

    ClientThread(SocketThreadListener listener, String name, Socket socket) {
        super(listener, name, socket);
        nickname = "Anon" +socket.getInetAddress();
    }

    void authAccept(String username){
        isAuthorized = true;
        this.nickname = username;
    }

    void authFailed(String msg){
        sendMessage(msg);
        close();
    }

    void msgFormatError(String msg){
        sendMessage(msg);
        close();
    }

    boolean isAuthorized() {
        return isAuthorized;
    }

    public int getAccessLevel() {
        return accessLevel;
    }

    String getNickname() {
        return nickname;
    }

    void setAccessLevel(int accessLevel) {
        this.accessLevel = accessLevel;
    }
}
