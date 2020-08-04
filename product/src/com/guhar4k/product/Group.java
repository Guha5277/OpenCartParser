package com.guhar4k.product;

import java.util.ArrayList;

public class Group {
    private Group parentGroup;
    private ArrayList<Group> childGroups;
    private ArrayList<Product> products;
    private final String name;
    private final String URL;

    public Group(String name, String URL) {
        this.name = name;
        this.URL = URL;
    }

    public Group(String name, String URL, Group parentGroup) {
        this(name, URL);
        this.parentGroup = parentGroup;
    }

    public void setParentGroup(Group parentGroup) {
        this.parentGroup = parentGroup;
    }

    public void addChild(Group childGroup) {
        if(childGroups == null){
            childGroups = new ArrayList<>();
        }
        childGroups.add(childGroup);
    }

    public void addLiquid(Product product){
        if (products == null){
            products = new ArrayList<>();
        }
        products.add(product);
    }

    public String getName() {
        return name;
    }

    public String getGroupURL() {
        return URL;
    }

    public ArrayList<Product> getProducts() {
        return products;
    }

    public Group parent(){
        return parentGroup;
    }

    public Group child(int index){
        return childGroups.get(index);
    }

    public ArrayList<Group> getChildGroups() {
        return childGroups;
    }

    public boolean isGroupEmpty(){
        return (childGroups == null && products == null);
    }

    public boolean isGroupHaveChild() {
        return !(childGroups == null);
    }

    public boolean isGroupHaveLiquids(){
        return !(products == null);
    }

    @Override
    public String toString() {
        return "<<product.Group: " + name
                +"\n\tURL: " + URL
                +"\n\tparentGroup: " + parentGroup + ">>";

    }
}
