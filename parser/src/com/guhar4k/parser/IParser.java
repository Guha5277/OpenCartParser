package com.guhar4k.parser;

public interface IParser {
    int getCategoryID(String productName);
    void insertProduct(String name, String url, int price, int categoryID, String name1, int volume, double strength);
}
