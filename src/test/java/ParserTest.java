import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

public class ParserTest {
    static Parser parser;
    //String URL = "https://ilfumoshop.ru";

    @BeforeClass
    public static void initialize() {
        parser = new Parser();
    }

    @Test
    public void remotePageNotNullTest() {
        Document retrievedDocument = parser.getFullPage();
        Assert.assertNotNull(retrievedDocument);
    }

    @Test
    public void receivedLiquidPartOfPage(){
        Elements retrievedElements = parser.getCategoriesPage();
        int count = retrievedElements.size();
        Assert.assertNotNull(retrievedElements);
        Assert.assertEquals(9, count);
    }

    @Test
    public void receivedCategoryData(){
        ArrayList<Category> list = parser.parseCategories();
        Assert.assertEquals(9, list.size());
    }
}