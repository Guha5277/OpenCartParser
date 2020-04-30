import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import product.Group;
import product.Product;

public class ProductTest {
    String name = "Жидкость Arctica 120 мл Currant Grapefruit 3 мг/мл";
    String url = "https://ilfumoshop.ru/zhidkost-arctica-120-ml-currant-grapefruit-3-mgml";
    Product product;

    @Before
    public void init() {
        product = new Product(name, url, 100);
    }

    @Test
    public void getName() {
        String receivedName = product.getName();
        Assert.assertEquals(name, receivedName);
    }

    @Test
    public void getURL() {
        String receivedUrl = product.getURL();
        Assert.assertEquals(url, receivedUrl);
    }

    @Test
    public void getNullGroupTest() {
        Group receivedGroup = product.getGroup();
        Assert.assertNull(receivedGroup);
    }

    @Test
    public void getNotNullGroupTest() {
        product.setGroup(new Group(name, url));
        Group receivedGroup = product.getGroup();
        Assert.assertNotNull(receivedGroup);
    }
}