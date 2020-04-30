import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Controller implements ParserEvents {
    Logger LOG = LogManager.getLogger();

    public static void main(String[] args) {
        Researcher researcher = new Researcher("https://ilfumoshop.ru/zhidkost-dlya-zapravki-vejporov.html", new Controller());
        new Thread(researcher).start();
    }

    @Override
    public void onParserReady() {
        SQLClient.connect();
        LOG.info("Parser Started");
    }

    @Override
    public void onUpdaterReady() {
        SQLClient.connect();
        LOG.info("Updater Started");
    }

    @Override
    public void onResearcherReady() {
        SQLClient.connect();
        LOG.info("Researcher Started");
    }

    @Override
    public void onParserException(Exception e) {
        LOG.error("Parser failed with: " + e.getMessage());
    }

    @Override
    public void onParseError() {
        LOG.error("Parser failed!");
    }

    @Override
    public void onUpdateError() {
        LOG.error("Updater failed!");
    }

    @Override
    public void onResearchError() {
        LOG.error("Researcher failed!");
    }

    @Override
    public void onParseSuccessfulEnd(int count) {
        LOG.info("Parser successfully end! Total products found: " + count);
        if (count > 0) SQLClient.commit();
        SQLClient.disconnect();
    }

    @Override
    public void onUpdateSuccessfulEnd(int count) {
        LOG.info("Updater successfully end! Total products updated: " + count);
        if (count > 0) SQLClient.commit();
        SQLClient.disconnect();
    }

    @Override
    public void onResearchSuccessfulEnd(int count) {
        LOG.info("Updater successfully end! Total products updated: " + count);
        if (count > 0) SQLClient.commit();
        SQLClient.disconnect();
    }
}
