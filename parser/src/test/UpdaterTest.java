import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class UpdaterTest {

    @Before
    public void init(){
        SQLClient.connect();
    }

    @After
    public void disable(){
        SQLClient.disconnect();
    }

    @Test
    public void test1() {
        Updater updater = new Updater(new ParserEvents() {
            @Override
            public void onParserReady() {

            }

            @Override
            public void onUpdaterReady() {

            }

            @Override
            public void onResearcherReady() {

            }

            @Override
            public void onParserException(Exception e) {

            }

            @Override
            public void onParseError() {

            }

            @Override
            public void onUpdateError() {

            }

            @Override
            public void onResearchError() {

            }

            @Override
            public void onParseSuccessfulEnd(int count) {

            }

            @Override
            public void onUpdateSuccessfulEnd(int count) {

            }

            @Override
            public void onResearchSuccessfulEnd(int count) {

            }
        });
        updater.test();
    }
}