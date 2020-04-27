import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SQLClientTest {

    @Before
    public void init(){
        SQLClient.connect();
    }

    @After
    public void disableAll(){
        SQLClient.disconnect();
    }

    @Test
    public void getAllProducts(){
        SQLClient.getAllProducts();
    }

}