package com.guhar4k.client.gui;

import java.time.LocalTime;
import java.util.List;

public interface GUIEvents {
    void onGUIReady();
    void onConnectButtonEvent(String ip, int port, String login, String password);
    void onAppCloseRequest();
    void startUpdaterRequest(boolean selected);
    void stopUpdaterRequest();
    void startResearcherRequest();
    void stopResearcherRequest();
    void kickUserRequest(String nickname);
    List storeListRequest(String selectedItem);
    void getImageRequest(int id);
    void applySettingsRequest(boolean updaterEnable, boolean researcherEnable, int updaterInterval, int researcherInterval, LocalTime updaterTime, LocalTime researcherTime);
    void productsRequest(boolean stockChecked, String cityName, String storeName, int strengthStart, int strengthEnd, int volumeStart, int volumeEnd, int priceStart, int priceEnd);
}
