class Product {
    private final String name;
    private final String URL;
    private int id;
    private int categoryID;
    private String categoryName;
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

    Product(String name, String URL, int price, Group group, String categoryName) {
        this(name, URL, price);
        this.group = group;
        this.categoryName = categoryName;
    }

    Product(int id, String name, String URL, int price, Group group, int categoryID) {
        this(name, URL, price, group, categoryID);
        this.id = id;
    }

    String getName() {
        return name;
    }

    public String getCategoryName() {
        return categoryName;
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

    int getId() {
        return id;
    }

    int getCategoryID() {
        return categoryID;
    }

    void setCategoryID(int categoryID) {
        this.categoryID = categoryID;
    }

    void setGroup(Group group) {
        this.group = group;
    }

    @Override
    public String toString() {
        return "<<Product: " + name
                + "\n\tURL: " + URL
                + "\n\tID: " + id
                + "\n\tCategoryID: " + categoryID
                + "\n\tgroup: " + group
                + "\n\tprice: " + price + ">>";
    }
}
