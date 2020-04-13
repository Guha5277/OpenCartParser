import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

public class GroupTest {
    String name = "Test Group";
    String url = "http://ilfumo/testGroup.html";
    Group group;

    @Before
    public void init(){
        group = new Group(name, url);
    }

    @Test
    public void receivedNameTest(){
        String receivedName = group.getGroupName();
        Assert.assertEquals(name, receivedName);
    }

    @Test
    public void receivedURLTest(){
        String receivedURL = group.getGroupURL();
        Assert.assertEquals(url, receivedURL);
    }

    @Test
    public void setParentTest(){
        String parentName = "Test Parent Name";
        String parentUrl = "http://ilfumo.ru/testParentGroup.html";
        group.setParentGroup(new Group(parentName, parentUrl));

        Group receivedGroup = group.parent();

        Assert.assertEquals(parentName, receivedGroup.getGroupName());
        Assert.assertEquals(parentUrl, receivedGroup.getGroupURL());
    }

    @Test
    public void addChildrenTest(){
        String childName1 = "Child1";
        String childName2 = "Child2";
        String childName3 = "Child3";
        String childURL1 = "http://ilfumo.ru/child1";
        String childURL2 = "http://ilfumo.ru/child2";
        String childURL3 = "http://ilfumo.ru/child3";

        group.addChild(new Group(childName1, childURL1));
        group.addChild(new Group(childName2, childURL2));
        group.addChild(new Group(childName3, childURL3));

        Group receivedChild1 = group.child(0);
        Group receivedChild2 = group.child(1);
        Group receivedChild3 = group.child(2);

        Assert.assertEquals(childName1, receivedChild1.getGroupName());
        Assert.assertEquals(childName2, receivedChild2.getGroupName());
        Assert.assertEquals(childName3, receivedChild3.getGroupName());
    }

    @Test (expected = NullPointerException.class)
    public void testChildNull(){
        group.child(0);
    }
}