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

    @Test
    public void isProductAlreadyInDB(){
       boolean receivedResult =  SQLClient.isProductAlreadyInDB("https://ilfumoshop.ru/zhidkost-arctica-120-ml-bubble-berry-3-mgml");
        assertTrue(receivedResult);
    }

    @Test
    public void isProductNotInDB(){
        boolean receivedResult =  SQLClient.isProductAlreadyInDB("some wrong url product");
        assertFalse(receivedResult);
    }
}