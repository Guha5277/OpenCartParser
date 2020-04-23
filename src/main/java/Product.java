class Product {
    private final String name;
    private final String URL;
    private int id;
    private int categoryID;
    private Group group;
    private final int price;

    Product(String name, String URL, int price) {
        this.name = name;
        this.URL = URL;
        this.price = price;
    }

    Product(String name, String URL, int price, Group group, int categoryID) {
        this(name, URL, price);
        this.group = group;
        this.categoryID = categoryID;
    }

    Product(int id, String name, String URL, int price, Group group, int categoryID) {
        this(name, URL, price, group, categoryID);
        this.id = id;
    }

    String getName() {
        return name;
    }

    String getURL() {
        return URL;
    }

    int getPrice() {
        return price;
    }

    Group getGroup() {
        return group;
    }

    public int getId() {
        return id;
    }

    int getCategoryID() {
        return categoryID;
    }

    public void setCategoryID(int categoryID) {
        this.categoryID = categoryID;
    }

    void setGroup(Group group) {
        this.group = group;
    }
}
