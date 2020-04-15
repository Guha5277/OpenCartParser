import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class CategoryTest {
    Category category;
    String name = "Жижа";
    String url = "https://yourdomain.com";
    @Before
    public void init(){
        category = new Category(name, url);
    }

    @Test
    public void getName() {
        String receivedName = category.getName();
        Assert.assertEquals(name, receivedName);
    }

    @Test
    public void getUrl() {
        String receivedUrl = category.getUrl();
        Assert.assertEquals(url, receivedUrl);
    }

    @Test
    public void nullGroupTest(){
        ArrayList<Group> receivedGroupList = category.getGroups();
        Assert.assertNull(receivedGroupList);
    }

    @Test
    public void addAndGetGroups() {
        category.addGroup(new Group(name, url));
        ArrayList<Group> receivedGroupList = category.getGroups();
        Assert.assertNotNull(receivedGroupList);
        Assert.assertEquals(1, receivedGroupList.size());
    }
}