public class Liquid extends Group{
    private final String name;
    private final String URL;

    public Liquid(String name, String URL) {
        super(name, URL);
        this.name = name;
        this.URL = URL;
    }

    public String getName() {
        return name;
    }

    public String getURL() {
        return URL;
    }
}
