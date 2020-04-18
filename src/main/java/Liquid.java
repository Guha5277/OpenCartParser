class Liquid{
    private final String name;
    private final String URL;
    private Group parentGroup;
    private Group group;
    private final int price;

    Liquid(String name, String URL, int price) {
        this.name = name;
        this.URL = URL;
        this.price = price;
    }

    Liquid(String name, String URL, int price, Group group) {
        this(name, URL, price);
        this.group = group;
    }

    Liquid(String name, String URL, int price, Group parentGroup, Group group) {
        this(name, URL, price);
        this.parentGroup = parentGroup;
        this.group = group;
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

    void setParentGroup(Group parentGroup) {
        this.parentGroup = parentGroup;
    }

    void setGroup(Group group) {
        this.group = group;
    }
}
