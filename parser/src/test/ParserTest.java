import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import product.Category;
import product.Product;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class ParserTest {
    Parser parser;
    String title = "Жидкость для Вейпов купить в Новосибирске, Москве, Омске, Томске, Красноярксе, Калининграде для электронных сигарет и вейпа";
    String productsPage = "https://ilfumoshop.ru/zhidkost-dlya-zapravki-vejporov.html";
    String singleProductPage = "https://ilfumoshop.ru/zhidkost-freaky-squeeze-60-ml-chilled-chillin-oxlazhdennaya-kola-s-ananasom-3-mgml";
    String singleProductName = "Жидкость FREAKY SQUEEZE 60 мл Chilled Chillin Охлажденная Кола с Ананасом 3 мг/мл";


    @Before
    public void init() {
        parser = new Parser();
    }

    @Test
    public void downloadPage() throws IOException {
        Document doc = parser.downloadPage(productsPage);
        String receivedTitle = doc.title();
        assertEquals(title, receivedTitle);
    }

    @Test
    public void getCategories() throws IOException  {
        Document doc = parser.downloadPage(productsPage);
        ArrayList<Category> receivedList = parser.getCategories(doc);
        assertEquals(9, receivedList.size());
    }

    @Test
    public void getCategoriesID()  throws IOException  {
        Document doc = parser.downloadPage(productsPage);
        ArrayList<Category> receivedList = parser.getCategories(doc);
        SQLClient.connect();
        parser.getCategoriesID(receivedList);
        SQLClient.disconnect();
        assertEquals(1, receivedList.get(0).getCategoryID());
    }

    @Test
    public void parseProductUrl() {
        Product receivedProduct = parser.parseProduct(singleProductPage);
        String receivedProductURL = receivedProduct.getURL();
        assertEquals(singleProductPage, receivedProductURL);
    }

    @Test
    public void parseProductName() {
        Product receivedProduct = parser.parseProduct(singleProductPage);
        String receivedName = receivedProduct.getName();
        assertEquals(singleProductName, receivedName);
    }

    @Test
    public void parseProductCategory() {
        SQLClient.connect();
        Product receivedProduct = parser.parseProduct(singleProductPage);
        SQLClient.disconnect();
        int receivedCategory = receivedProduct.getCategoryID();
        assertEquals(1, receivedCategory);
    }
}