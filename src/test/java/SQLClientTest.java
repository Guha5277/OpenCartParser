import org.junit.Test;

import static org.junit.Assert.*;

public class SQLClientTest {

    @Test
    public void connect() {
        SQLClient.connect();
    }

    @Test
    public void disconnect() {
        SQLClient.disconnect();
    }
}