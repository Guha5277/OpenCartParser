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
    String singleProductName = "Жидкость FREAKY SQUEEZE Chilled Chillin Охлажденная Кола с Ананасом";


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
        SQLClient.connect();
        Product receivedProduct = parser.parseProduct(singleProductPage);
        String receivedProductURL = receivedProduct.getURL();
        SQLClient.disconnect();
        assertEquals(singleProductPage, receivedProductURL);
    }

    @Test
    public void parseProductName() {
        SQLClient.connect();
        Product receivedProduct = parser.parseProduct(singleProductPage);
        SQLClient.disconnect();
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


//    @Test
//    public void parseProductVolume() {
//        int receivedVolume = parser.parseVolume("Жидкость Coil Sauz Strawberry Cannoli 100 мл 0 мг/мл");
//        assertEquals(100, receivedVolume);
//    }

//    @Test
//    public void parseProductVolume2() {
//        int receivedVolume = parser.parseVolume("Жидкость Candy Juice SALT 30мл Apple 25 мг/мл");
//        assertEquals(30, receivedVolume);
//    }

    @Test
    public void parseProductStrength() {
        double receivedStrength = parser.parseStrength("Жидкость Coil Sauz Strawberry Cannoli 100 мл 0 мг/мл");
        assertEquals(0.0, receivedStrength, 0.0);
    }

    @Test
    public void trimToValidName() {
        String receivedName = parser.trimToValidName("Жидкость Coil Sauz Strawberry Cannoli 100 мл 0 мг/мл", 100, 0.0d);
        assertEquals("Жидкость Coil Sauz Strawberry Cannoli", receivedName);
    }

    @Test
    public void trimToValidNameTwo() {
        String receivedName = parser.trimToValidName("Жидкость Heroes 2*60 мл Clash: Battle Drunk Grape Rifle 3 мг/мл", 120, 3.0d);
        assertEquals("Жидкость Heroes Clash: Battle Drunk Grape Rifle", receivedName);
    }
}