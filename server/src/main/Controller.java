import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Controller implements ParserEvents {
    Logger LOG = LogManager.getLogger();

    public static void main(String[] args) {
        SQLClient.connect();
        Grabber grabber = new Grabber("https://ilfumoshop.ru/zhidkost-dlya-zapravki-vejporov.html", new Controller());
        new Thread(grabber).start();
    }

    @Override
    public void onParseStarted() {
        LOG.info("Parser Started");
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
    public void onParseSuccessfulEnd(int count) {
        SQLClient.commit();
        LOG.info("Parser successfully end! Total products found: " + count);
        SQLClient.disconnect();
    }

    @Override
    public void onUpdateSuccessfulEnd(int count) {
        SQLClient.commit();
        LOG.info("Updater successfully end! Total products updated: " + count);
        SQLClient.disconnect();
    }
}
