package main;

import com.google.gson.JsonSyntaxException;
import com.guhar4k.library.DataProtocol;
import com.guhar4k.library.Library;
import com.guhar4k.library.ProductRequest;

import java.time.LocalTime;

public class MessageHandler implements MessageHandlerImpl {
    private MessageHandlerListener listener;
    private final int UPDATER_HEADER_LENGTH = 2;
    private final int RESEARCHER_HEADER_LENGTH = 2;
    private final int KICK_USER_HEADER_LENGTH = 2;
    private final int PRODUCT_REQUEST_HEADER_LENGTH = 2;
    private final int UPDATER_PERMISSION_REQUIRED = 2;
    private final int RESEARCHER_PERMISSION_REQUIRED = 2;
    private final int INVALID_VALUE = -1;

    public MessageHandler(MessageHandlerListener listener) {
        this.listener = listener;
    }

    @Override
    public void handleMessage(SocketThread socketThread, String msg) {
        DataProtocol receivedData;
        ClientThread client = (ClientThread) socketThread;
        try {
            receivedData = Library.jsonToObject(msg);
        } catch (com.google.gson.JsonSyntaxException e) {
            listener.messageFormatException(client);
            return;
        }

        byte[] header = receivedData.getHeader();
        String message = receivedData.getData();

        switch (header[0]) {
            case Library.AUTH:
                handleAuthRequest(client, message);
                break;
            case Library.SERVER_INFO:
                handleServerInfoRequest(client);
                break;
            case Library.UPDATER:
                handleUpdaterRequest(client, header, message);
                break;
            case Library.RESEARCHER:
                handleResearcherRequest(client, header, message);
                break;
            case Library.USERS:
                handleUsersModerationRequest(client, header, message);
                break;
            case Library.PRODUCT_REQUEST:
                handleProductRequest(client, header, message);
                break;
            case Library.REMAINS:
                handleRemainsRequest(client, message);
                break;
            case Library.IMAGE:
                handleImageRequest(client, message);
                break;
        }
    }

    private void handleAuthRequest(ClientThread client, String msg) {
        if (!isValidAuthRequest(msg)) {
            listener.messageFormatException(client);
            return;
        }

        String[] loginPasswordArray = msg.split(Library.DELIMITER);
        String login = loginPasswordArray[0];
        String password = loginPasswordArray[1];
        listener.authRequest(client, login, password);
    }

    private boolean isValidAuthRequest(String msg) {
        return msg.length() >= 10 && !msg.contains(" ") && msg.contains(Library.DELIMITER);
    }

    private void handleServerInfoRequest(ClientThread client) {
        int accessLevel = client.getAccessLevel();
        if (accessLevel == ClientThread.ADMIN || accessLevel == ClientThread.MODERATOR)
            listener.serverInfoForAdmin(client);
        else listener.serverInfoForGuest(client);
    }

    private void handleUpdaterRequest(ClientThread client, byte[] header, String message) {
        if (!isHeaderLengthValid(client, header, UPDATER_HEADER_LENGTH) || !checkPermission(client, UPDATER_PERMISSION_REQUIRED))
            return;

        switch (header[1]) {
            case Library.START:
                listener.onUpdaterStartRequest(Boolean.valueOf(message));
                break;
            case Library.STOP:
                listener.onUpdaterStopRequest();
                break;
            case Library.AUTOSTART:
                handleUpdaterAutostartStatus(message);
                break;
            case Library.AUTOSTART_INTERVAL:
                handleUpdaterAutostartInterval(client, message);
                break;
            case Library.AUTOSTART_TIME:
                handleUpdaterAutostartTime(client, message);
                break;
        }
    }

    private void handleResearcherRequest(ClientThread client, byte[] header, String message) {
        if (!isHeaderLengthValid(client, header, RESEARCHER_HEADER_LENGTH) || !checkPermission(client, RESEARCHER_PERMISSION_REQUIRED))
            return;

        switch (header[1]) {
            case Library.START:
                listener.onResearcherStartRequest();
                break;
            case Library.STOP:
                listener.onResearcherStopRequest();
                break;
            case Library.AUTOSTART:
                handleResearcherAutostartStatus(message);
                break;
            case Library.AUTOSTART_INTERVAL:
                handleResearcherAutostartInterval(client, message);
                break;
            case Library.AUTOSTART_TIME:
                handleResearcherAutostartTime(client, message);
                break;
        }
    }

    private boolean checkPermission(ClientThread client, int requiredPermission) {
        if (client.getAccessLevel() > requiredPermission) {
            listener.permissionDenied(client);
            return false;
        }
        return true;
    }

    private boolean isHeaderLengthValid(ClientThread client, byte[] header, int length) {
        if (header.length != length) {
            listener.messageFormatException(client);
            return false;
        }
        return true;
    }

    private void handleUpdaterAutostartStatus(String message) {
        boolean status = Boolean.valueOf(message);
        listener.onUpdaterAutostartStateChanged(status);
    }

    private void handleResearcherAutostartStatus(String message) {
        boolean status = Boolean.valueOf(message);
        listener.onResearcherAutostartStateChanged(status);
    }

    private void handleUpdaterAutostartInterval(ClientThread client, String message) {
        int interval = parseInterval(message);
        if (interval == INVALID_VALUE) {
            listener.messageFormatException(client);
            //TODO add logs
            return;
        }
        listener.onUpdaterAutostartIntervalChanged(interval);
    }

    private void handleResearcherAutostartInterval(ClientThread client, String message) {
        int interval = parseInterval(message);
        if (interval == INVALID_VALUE) {
            listener.messageFormatException(client);
            //TODO add logs
            return;
        }
        listener.onResearcherAutostartIntervalChanged(interval);
    }

    private void handleUpdaterAutostartTime(ClientThread client, String message) {
        LocalTime autostartTime = parseTime(message);
        if (autostartTime == null) {
            listener.messageFormatException(client);
            //TODO add logs
            return;
        }
        listener.onUpdaterAutostartTimeChanged(autostartTime);
    }

    private void handleResearcherAutostartTime(ClientThread client, String message) {
        LocalTime autostartTime = parseTime(message);
        if (autostartTime == null) {
            listener.messageFormatException(client);
            //TODO add logs
            return;
        }
        listener.onResearcherAutostartTimeChanged(autostartTime);
    }

    private int parseInterval(String intervalPart) {
        int interval;
        try {
            interval = Integer.parseInt(intervalPart);
        } catch (NumberFormatException e) {
            return INVALID_VALUE;
        }
        return interval;
    }

    private LocalTime parseTime(String timePart) {
        LocalTime autostartTime;
        try {
            autostartTime = LocalTime.parse(timePart);
        } catch (RuntimeException e) {
            return null;
        }
        return autostartTime;
    }

    private void handleUsersModerationRequest(ClientThread client, byte[] header, String message) {
        ClientThread targetUser = listener.getUser(message);
        if (!isHeaderLengthValid(client, header, KICK_USER_HEADER_LENGTH) || message == null || targetUser == null) {
            listener.messageFormatException(client);
            return;
        }

        if (client.getAccessLevel() >= targetUser.getAccessLevel()) {
            listener.permissionDenied(client);
            return;
        }

        switch (header[1]) {
            case Library.KICK:
                listener.onKickUserRequest(client, targetUser);
                break;
            case Library.BAN:
                listener.onBanUserRequest(client, targetUser);
                break;
        }
    }

    private void handleProductRequest(ClientThread client, byte[] header, String message) {
        if (!isHeaderLengthValid(client, header, PRODUCT_REQUEST_HEADER_LENGTH)) {
            listener.messageFormatException(client);
            return;
        }

        switch (header[1]) {
            case Library.SORT:
                handleSortProduct(client, message);
                break;
            case Library.NEW:
                handleNewProductRequest(client, message);
                break;
            case Library.NEXT:
                listener.onNextPageProductRequest(client);
                break;
            case Library.DAILY_OFFER:
                listener.onDailyOfferRequest(client);
                break;
        }
    }

    private void handleSortProduct(ClientThread client, String message) {
        if (message == null) {
            listener.messageFormatException(client);
            return;
        }

        int sortType;
        try {
            sortType = Integer.parseInt(message);
        } catch (NumberFormatException e) {
            listener.messageFormatException(client);
            return;
        }
        listener.onSortProductsRequest(client, sortType);
    }

    private void handleNewProductRequest(ClientThread client, String message) {
        if (message == null) {
            listener.messageFormatException(client);
            return;
        }

        ProductRequest productRequest;
        try {
            productRequest = Library.productRequestFromJson(message);
        } catch (JsonSyntaxException e) {
            listener.messageFormatException(client);
            return;
        }
        int sortType = productRequest.getSortType();
        listener.onNewProductRequest(client, productRequest, sortType);
    }

    private void handleRemainsRequest(ClientThread client, String message) {
        if (message == null) {
            listener.messageFormatException(client);
            return;
        }
        try {
            int productID = Integer.parseInt(message);
            listener.onProductRemainsRequest(client, productID);
        } catch (NumberFormatException e) {
            listener.messageFormatException(client);
        }
    }

    private void handleImageRequest(ClientThread client, String message) {
        if (message == null) {
            listener.messageFormatException(client);
            return;
        }
        try {
            int productID = Integer.parseInt(message);
            listener.onProductImageRequest(client, productID);
        } catch (NumberFormatException e) {
            listener.messageFormatException(client);
        }
    }
}
