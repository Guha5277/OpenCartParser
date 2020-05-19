package main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Library {
    public static final int ADMIN = 1;
    public static final int MODERATOR = 2;

    public static final String DELIMITER = "§";
    public static final byte AUTH = 1;
    public static final byte REQUEST = 100;
    public static final byte ACCEPTED = 101;
    public static final byte DENIED = 102;
    public static final byte MESSAGE_FORMAT_ERROR = -1;
    public static final byte GET_SERVER_INFO = 2;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static String getAuthRequest(String login, String password){
        return makeJsonString(Library.AUTH, Library.REQUEST, login + DELIMITER + password);
    }

    public static DataProtocol jsonToObject(String json){
        return GSON.fromJson(json, DataProtocol.class);
    }

    public static String makeJsonString(byte header1, byte header2, String data) {
        DataProtocol message = new DataProtocol(new byte[]{header1, header2}, data);
        return GSON.toJson(message);
    }

    public static String makeJsonString(byte header1, byte header2) {
        DataProtocol message = new DataProtocol(new byte[]{header1, header2});
        return GSON.toJson(message);
    }

    public static String makeJsonString(byte header, String data) {
        DataProtocol message = new DataProtocol(new byte[]{header}, data);
        return GSON.toJson(message);
    }

    public static String makeJsonString(byte header) {
        DataProtocol message = new DataProtocol(new byte[]{header});
        return GSON.toJson(message);
    }
}
