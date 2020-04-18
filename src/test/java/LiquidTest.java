import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class LiquidTest {
    String name = "Жидкость Arctica 120 мл Currant Grapefruit 3 мг/мл";
    String url = "https://ilfumoshop.ru/zhidkost-arctica-120-ml-currant-grapefruit-3-mgml";
    Liquid liquid;

    @Before
    public void init() {
        liquid = new Liquid(name, url, 100);
    }

    @Test
    public void getName() {
        String receivedName = liquid.getName();
        Assert.assertEquals(name, receivedName);
    }

    @Test
    public void getURL() {
        String receivedUrl = liquid.getURL();
        Assert.assertEquals(url, receivedUrl);
    }

    @Test
    public void getNullGroupTest() {
        Group receivedGroup = liquid.getGroup();
        Assert.assertNull(receivedGroup);
    }

    @Test
    public void getNotNullGroupTest() {
        liquid.setGroup(new Group(name, url));
        Group receivedGroup = liquid.getGroup();
        Assert.assertNotNull(receivedGroup);
    }
}