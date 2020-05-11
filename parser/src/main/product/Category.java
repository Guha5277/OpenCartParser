package main.product;

import java.util.ArrayList;

public class Category {
    private final String name;
    private final String url;
    private int categoryID;
    private ArrayList<Group> groups;

    public Category(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public void setCategoryID(int id){
        categoryID = id;
    }

    public void addGroup(Group group){
        if (groups == null){
            groups = new ArrayList<>();
        }
        groups.add(group);
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public int getCategoryID() {
        return categoryID;
    }

    public ArrayList<Group> getGroups() {
        return groups;
    }
}
