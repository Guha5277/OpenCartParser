package product;

public class Product {
    private String name;
    private final String URL;
    private int id;
    private int categoryID;
    private String categoryName;
    private Group group;
    private final int price;
    private int volume;
    private double strength;

    public Product(String name, String URL, int price) {
        this.name = name;
        this.URL = URL;
        this.price = price;
    }

    public Product(String name, String URL, int price, Group group, int categoryID, int volume, double strength) {
        this(name, URL, price);
        this.group = group;
        this.categoryID = categoryID;
        this.volume = volume;
        this.strength = strength;
    }

    Product(String name, String URL, int price, Group group, String categoryName) {
        this(name, URL, price);
        this.group = group;
        this.categoryName = categoryName;
    }

    public Product(int id, String name, String URL, int price, Group group, int categoryID) {
        this(name, URL, price);
        this.id = id;
        this.group = group;
        this.categoryID = categoryID;
    }

    public String getName() {
        return name;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getURL() {
        return URL;
    }

    public int getPrice() {
        return price;
    }

    public Group getGroup() {
        return group;
    }

    public int getVolume() {
        return volume;
    }

    public double getStrength() {
        return strength;
    }

    public int getId() {
        return id;
    }

    public int getCategoryID() {
        return categoryID;
    }

    public void setCategoryID(int categoryID) {
        this.categoryID = categoryID;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public void setStrength(double strength) {
        this.strength = strength;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "<<product.Product: " + name
                + "\n\tURL: " + URL
                + "\n\tID: " + id
                + "\n\tCategoryID: " + categoryID
                + "\n\tgroup: " + group
                + "\n\tprice: " + price
                + "\n\tvolume: " + volume
                + "\n\tstrength: " + strength + ">>";
    }
}
