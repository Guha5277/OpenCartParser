package com.guhar4k.product;

import java.util.ArrayList;

public class DailyOffer {
    private final String name;
    private ArrayList<Product> products;
    private boolean isAllImageReady;
    private boolean isAllProductReceived;

    public DailyOffer(String name, Product product) {
        this.name = name;
        this.products = new ArrayList<>();
        this.products.add(product);
    }

    public String getName() {
        return name;
    }

    public ArrayList<Product> getProductsList() {
        return products;
    }

    public void addProduct(Product product){
        products.add(product);
    }

    public boolean isAllImageReady(){
        return isAllImageReady;
    }

    public boolean containsProduct(int productID){
        for (Product p : products){
            if (p.getId() == productID) return true;
        }
        return false;
    }

    public boolean isAllProductReceived() {
        return isAllProductReceived;
    }

    public boolean isReady() {
        return isAllProductReceived && isAllImageReady;
    }
}
