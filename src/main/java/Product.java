class Product {
    private final String name;
    private final String URL;
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
