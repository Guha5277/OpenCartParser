package main;

import com.guhar4k.library.ProductRequest;

import java.time.LocalTime;

public interface MessageHandlerListener {

    void messageFormatException(ClientThread client);
    void permissionDenied(ClientThread client);

    void authRequest(ClientThread client, String login, String password);
    void serverInfoForAdmin(ClientThread client);
    void serverInfoForGuest(ClientThread client);

    void onUpdaterStartRequest();
    void onUpdaterStopRequest();
    void onUpdaterAutostartStateChanged(boolean status);
    void onUpdaterAutostartIntervalChanged(int interval);
    void onUpdaterAutostartTimeChanged(LocalTime autostartTime);

    void onResearcherStartRequest();
    void onResearcherStopRequest();
    void onResearcherAutostartStateChanged(boolean status);
    void onResearcherAutostartIntervalChanged(int interval);
    void onResearcherAutostartTimeChanged(LocalTime autostartTime);

    ClientThread getUserRole(String nickname);
    void onKickUserRequest(ClientThread client, ClientThread targetUser);
    void onBanUserRequest(ClientThread client, ClientThread targetUser);

    void onSortProductsRequest(ClientThread client, int sortType);
    void onNewProductRequest(ClientThread client, ProductRequest productRequest, int sortType);
    void onNextPageProductRequest(ClientThread client);

    void onProductRemainsRequest(ClientThread client, int productID);
    void onProductImageRequest(ClientThread client, int productID);
}
