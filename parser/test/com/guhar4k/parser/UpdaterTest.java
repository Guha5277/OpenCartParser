package com.guhar4k.parser;

import com.guhar4k.product.Group;
import com.guhar4k.product.Product;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UpdaterTest {
    private static Updater updater;
    private static int id = 1;
    private static String name = "Жижа";
    private static String name2 = "Жижаа";
    private static String URL = "http://";
    private static int price = 500;
    private static int price2 = 510;
    private static Group group = new Group("Some group", "http://");
    private static Group group2 = new Group("Some group2", "http://");
    private static int category = 0;
    private static int category2 = 1;
    private static int volume = 100;
    private static int volume2 = 120;
    private static double strength = 1.0d;
    private static double strength2 = 3.0d;

    private static Product product1;
    private static Product product2;
    private static Product product3;

    static IUpdater listener;

    @BeforeAll
    static void initAll() throws IOException {
        listener = mock(IUpdater.class);
        updater = new Updater(listener, 0, "", new ArrayList<>(), new ArrayList<>());
        product1 = new Product(id, name, URL, price, group, category, volume, strength, "");
        product2 = new Product(id, name2, URL, price2, group2, category2, volume2, strength2, "");
        product3 = new Product(id, name2, URL, price2, group2, category2, volume2, strength2, "");
    }

    @Test
    void compareProducts(){
        boolean result = updater.compareProduct(product1, product2);
        verify(listener, times(1)).updateProductName(id, name);
        verify(listener, times(1)).updateProductPrice(id, price);
        verify(listener, times(1)).updateProductGroupName(id, group.getName());
        verify(listener, times(1)).updateProductCategory(id, category);
        verify(listener, times(1)).updateProductVolume(id, volume);
        verify(listener, times(1)).updateProductStrength(id, strength);

        assertTrue(result);
    }

    @Test
    void compareEqualsProduct(){
        boolean result = updater.compareProduct(product2, product3);
        assertFalse(result);
    }
}