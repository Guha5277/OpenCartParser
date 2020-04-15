import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

public class ParserTest {
    static Parser parser;

    @BeforeClass
    public static void initialize() {
        parser = new Parser();
    }

    @Test
    public void receivedCategoryData(){
        ArrayList<Category> list = parser.getCategories();
        Assert.assertEquals(9, list.size());
    }

    @Test
    public void vailReceivedData(){
        ArrayList<Category> list = parser.getCategories();
        Assert.assertEquals("Жидкость Постоянного ассортимента", parser.getCategories().get(0).getName());
        Assert.assertEquals("Жидкость Америка", parser.getCategories().get(1).getName());
        Assert.assertEquals("Жидкость Дополнительного ассортимента", parser.getCategories().get(2).getName());
    }

//    @Test
//    public void getAndParseGroupsTest(){
//        parser.parseGroups();
//        ArrayList<Category> receivedCategories = parser.getCategories();
//
//        for(Category cat : receivedCategories){
//            ArrayList<Group> groups = cat.getGroups();
//            if(groups == null) continue;
//            for (Group g : groups){
//                System.out.println(g.getGroupName());
//            }
//        }
//    }

}