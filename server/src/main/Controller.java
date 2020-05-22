package main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Controller {
    Logger LOG = LogManager.getLogger();
    private final GUIEvents listener;
    private Updater updater;
    private Thread updaterThread;

    Controller(GUIEvents listener){
        this.listener = listener;
    }

    public static void main(String[] args) {
//        Grabber grabber = new Grabber("https://ilfumoshop.ru/zhidkost-dlya-zapravki-vejporov.html", new Controller());
//        new Thread(grabber).start();

//        Updater updater = new Updater(new Controller());
//        new Thread(updater).start();

//        Researcher researcher = new Researcher("https://ilfumoshop.ru/zhidkost-dlya-zapravki-vejporov.html", new Controller());
//        new Thread(researcher).start();
    }

//    void startUpdater(){
//        if (!isUpdaterAlive()){
//            updater = new Updater(this);
//            updaterThread = new Thread(updater);
//            updaterThread.start();
//        }
//    }
//
//    void stopUpdater(){
//        if (isUpdaterAlive()){
//            updater.stop();
//        }
//    }
//
//    boolean isUpdaterAlive(){
//        if (updaterThread == null) return false;
//        return updaterThread.isAlive();
//    }
//
//    //Updater Events
//    @Override
//    public void onUpdaterReady() {
//        SQLClient.connect();
//        LOG.info("Updater Started");
//        listener.onUpdaterStart();
//    }
//
//    @Override
//    public void onUpdateError() {
//        LOG.error("Updater failed!");
//    }
//
//
//    @Override
//    public void onUpdateSuccessfulEnd(int checked, int updated) {
//        LOG.info("Updater successfully end! Total products updated: " + updated);
//        if (updated > 0) SQLClient.commit();
//        SQLClient.disconnect();
//        listener.onUpdaterEnd(checked, updated);
//    }
//
//    @Override
//    public void onUpdateProductFailed(String URL, int errors) {
//        listener.onUpdaterProductFailed(URL);
//    }
//
//    @Override
//    public void onUpdaterTotalProducts(int count) {
//        listener.onUpdaterTotalProducts(count);
//    }
//
//    @Override
//    public void onUpdaterCurrentProduct(int position, String name) {
//        listener.onUpdaterCurrentProduct(position);
//    }
//
//    //Grabber Events
//
//    @Override
//    public void onGrabberReady() {
//        SQLClient.connect();
//        LOG.info("Parser Started");
//    }
//
//
//    @Override
//    public void onResearcherReady() {
//        SQLClient.connect();
//        LOG.info("Researcher Started");
//    }
//
//    @Override
//    public void onParserException(Exception e) {
//        LOG.error("Parser failed with: " + e.getMessage());
//    }
//
//    @Override
//    public void onGrabError() {
//        LOG.error("Parser failed!");
//    }
//
//
//
//    @Override
//    public void onResearchError() {
//        LOG.error("Researcher failed!");
//    }
//
//    @Override
//    public void onParseSuccessfulEnd(int count) {
//        LOG.info("Parser successfully end! Total products found: " + count);
//        if (count > 0) SQLClient.commit();
//        SQLClient.disconnect();
//    }
//
//
//
//    @Override
//    public void onResearchSuccessfulEnd(int count) {
//        LOG.info("Updater successfully end! Total products updated: " + count);
//        if (count > 0) SQLClient.commit();
//        SQLClient.disconnect();
//    }
}
