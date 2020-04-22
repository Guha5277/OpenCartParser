import java.util.ArrayList;

class Group {
    private Group parentGroup;
    private ArrayList<Group> childGroups;
    private ArrayList<Product> products;
    private final String name;
    private final String URL;

    Group(String name, String URL) {
        this.name = name;
        this.URL = URL;
    }

    Group(String name, String URL, Group parentGroup) {
        this(name, URL);
        this.parentGroup = parentGroup;
    }

    void setParentGroup(Group parentGroup) {
        this.parentGroup = parentGroup;
    }

    void addChild(Group childGroup) {
        if(childGroups == null){
            childGroups = new ArrayList<>();
        }
        childGroups.add(childGroup);
    }

    void addLiquid(Product product){
        if (products == null){
            products = new ArrayList<>();
        }
        products.add(product);
    }

    String getGroupName() {
        return name;
    }

    String getGroupURL() {
        return URL;
    }

    ArrayList<Product> getProducts() {
        return products;
    }

    Group parent(){
        return parentGroup;
    }

    Group child(int index){
        return childGroups.get(index);
    }

    public ArrayList<Group> getChildGroups() {
        return childGroups;
    }

    boolean isGroupEmpty(){
        return (childGroups == null && products == null);
    }

    boolean isGroupHaveChild() {
        return !(childGroups == null);
    }

    boolean isGroupHaveLiquids(){
        return !(products == null);
    }

}
