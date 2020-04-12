import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ParserTest {
    static Parser parser;
    //String URL = "https://ilfumoshop.ru";

    @BeforeClass
    public static void initialize() {
        parser = new Parser();
    }

    @Test
    public void remotePageNotNullTest() {
        Document retrievedDocument = parser.getPage();
        Assert.assertNotNull(retrievedDocument);
    }

    @Test
    public void retrievedCategoriesTest(){
        Elements retrievedElements = parser.getCategories();
        int count = retrievedElements.size();
        Assert.assertNotNull(retrievedElements);
        Assert.assertEquals(9, count);
    }

    @Test
    public void testOutput(){
        parser.showCategories();
    }
}