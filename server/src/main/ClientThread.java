package main;

import java.net.Socket;

public class ClientThread extends SocketThread {
    public static final int ADMIN = 1;
    public static final int MODERATOR = 2;
    public static final int USER = 3;
    public static final int GHOST = 4;
    private final int ERRORS_LIMIT = 10;

    private String nickname;
    private boolean isAuthorized;
    private int accessLevel = GHOST;

    private int errorsCount;

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

    void messageFormatException(String msg){
        sendMessage(msg);
        errorsCount++;
        checkErrorsLimit();
    }

    void permissionDenied(String msg){
        sendMessage(msg);
        errorsCount++;
        checkErrorsLimit();
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

    private void checkErrorsLimit(){
        if (errorsCount >= ERRORS_LIMIT) {
            close();
        }
    }
}
