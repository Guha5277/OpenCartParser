import java.util.ArrayList;

public class Category {
    private final String name;
    private final String url;
    private ArrayList<Group> groups;

    public Category(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}
