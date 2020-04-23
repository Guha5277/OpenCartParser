import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import java.util.ArrayList;

public class ParserTest {
    private static Parser parser;
    public String name = "testname";
    public String url = "testurl";

    @BeforeClass
    public static void initialize() {
        parser = new Parser();
    }

    @Test
    public void testTime(){
        parser = new Parser();
    }

    @Test
    public void receivedCategoryData() {
        ArrayList<Category> list = parser.getCategories();
        Assert.assertEquals(9, list.size());
    }

    @Test
    public void vailReceivedData() {
        ArrayList<Category> list = parser.getCategories();
        Assert.assertEquals("Жидкость Постоянного ассортимента", parser.getCategories().get(0).getName());
        Assert.assertEquals("Жидкость Америка", parser.getCategories().get(1).getName());
        Assert.assertEquals("Жидкость Дополнительного ассортимента", parser.getCategories().get(2).getName());
    }

//    @Test
//    public void productParse(){
//        String url = "https://ilfumoshop.ru/zhidkost-yummy-cold-watermelon-and-strawberry-100-ml-3-mgml.html";
//        Product receivedProd = parser.parseProduct(url, null, 2);
//        Assert.assertEquals(url, receivedProd.getURL());
//        Assert.assertEquals(1300, receivedProd.getPrice());
//    }
}