public class Liquid{
    private final String name;
    private final String URL;
    private final Group group;
    private final int price;

    public Liquid(String name, String URL, Group group, int price) {
        this.name = name;
        this.URL = URL;
        this.group = group;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public String getURL() {
        return URL;
    }

    public Group getGroup() {
        return group;
    }
}
