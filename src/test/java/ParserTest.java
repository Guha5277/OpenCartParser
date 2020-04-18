import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;

public class ParserTest {
    static Parser parser;
    public String name = "testname";
    public String url = "testurl";

    @BeforeClass
    public static void initialize() {
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

    @Test
    public void parseEmptyGroupTest() {
        ArrayList<Group> groups = new ArrayList<>();
        try {
            Elements els = Jsoup.connect("https://ilfumoshop.ru/zhidkost-ashtray.html").get().body().getElementsByClass("col-lg-4 col-md-4 col-sm-6 col-xs-12");
            for (Element el : els) {
                groups.add(parser.getGroupContains(el.child(1).select("a").get(0), new Group(name, url)));
            }
        } catch (
                IOException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(0, groups.size());

    }

    @Test
    public void getGroupContainsTest() {
        ArrayList<Group> groups = new ArrayList<>();
        try {
            Elements els = Jsoup.connect("https://ilfumoshop.ru/zhidkost-freaky-squeeze.html").get().body().getElementsByClass("col-lg-4 col-md-4 col-sm-6 col-xs-12");
            for (Element el : els) {
                groups.add(parser.getGroupContains(el.child(1).select("a").get(0), new Group(name, url)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Assert.assertNotNull(groups.get(0));
        Assert.assertNotNull(groups.get(1));
        Assert.assertNotNull(groups.get(0).getGroupName());

        Assert.assertEquals(2, groups.size());

        Group receivedParentGroup = groups.get(0).parent();
        Assert.assertEquals(name, receivedParentGroup.getGroupName());
        Assert.assertEquals(url, receivedParentGroup.getGroupURL());
    }

}