import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import java.util.ArrayList;

public class ParserOLDTest {
    private static ParserOLD parserOLD;
    public String name = "testname";
    public String url = "testurl";

    @BeforeClass
    public static void initialize() {
        parserOLD = new ParserOLD();
    }

    @Test
    public void testTime(){
        parserOLD = new ParserOLD();
    }

    @Test
    public void receivedCategoryData() {
        ArrayList<Category> list = parserOLD.getCategories();
        Assert.assertEquals(9, list.size());
    }

    @Test
    public void vailReceivedData() {
        ArrayList<Category> list = parserOLD.getCategories();
        Assert.assertEquals("Жидкость Постоянного ассортимента", parserOLD.getCategories().get(0).getName());
        Assert.assertEquals("Жидкость Америка", parserOLD.getCategories().get(1).getName());
        Assert.assertEquals("Жидкость Дополнительного ассортимента", parserOLD.getCategories().get(2).getName());
    }

//    @Test
//    public void productParse(){
//        String url = "https://ilfumoshop.ru/zhidkost-yummy-cold-watermelon-and-strawberry-100-ml-3-mgml.html";
//        Product receivedProd = parserOLD.parseProduct(url, null, 2);
//        Assert.assertEquals(url, receivedProd.getURL());
//        Assert.assertEquals(1300, receivedProd.getPrice());
//    }
}