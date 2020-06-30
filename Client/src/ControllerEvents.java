import javafx.scene.image.Image;
import main.product.Product;

import java.time.LocalTime;
import java.util.List;

interface ControllerEvents {
    void onLoginConfigLoaded(String ip, String port, boolean saveSetState, String login, String password);
    void onConnectFailed(String message);
    void onConnectLost();
    void onAuthAccepted(String nickname);
    void onAuthDenied();
    void onFailToGetServerInfo();
    void onServerInfoReceived(boolean isAdmin, String startTime, int productsCount, int warehousesCount, int clientsCount);
    void onMultiplySession(String nickname);
    void onReceiveCityName(String city);
    void onActiveUsersCountChanged(String count);
    void onUsersListUpdated(String[] users);
    void onKickUserFailed(String userNicknameToKick);
    void kickedFromTheServer(String initiator);
    void onUpdaterInfoReceived(String lastRunDate, boolean autostart, int interval, LocalTime autostartTime, boolean hasLastUpdatedPosition);
    void onResearcherInfoReceived(String lastRunDate, boolean autostart, int interval, LocalTime autostartTime);
    void onUpdaterStart();
    void updaterStopped();
    void onUpdaterProgressChanged(double progress, String progressLabelText, String name);
    void onUpdaterDifferencesFound(String count, String content);
    void updaterError(String URL, String failsCount);
    void onProductImageFound(int id, Image image);
    void onProductImageNotFound(String productID);
    void onResearcherStart();
    void onResearcherStopped();
    void onResearcherProgressChanged(double progress, String progressLabelText, String group);
    void onResearcherCurrentCategoryChanged(String position, String categoryName);
    void onResearcherNewProductFound(String product, String totalFound);
    void noSelectedProducts();
    void onProductRequestSent(boolean stock, String city, String store);
    void onUpdaterException(String productID, String productURL, String message);
    void onUpdaterException(String exception, String message);
    void onProductsNotFound();
    void onProductFound(Product product);
    void allProductsReceived(List<Product> products);
    void onServerUptimeUpdated(String serverTime);
    void onUpdaterLastRunChanged(String lastRunDate);
    void onUpdaterLastPositionChanged(boolean hasLastUpdatedPosition);
}
