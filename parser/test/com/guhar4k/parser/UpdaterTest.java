package com.guhar4k.parser;

import com.guhar4k.product.Group;
import com.guhar4k.product.Product;
import com.guhar4k.product.Warehouse;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpdaterTest {
    private static Updater updater;

    @BeforeAll
    static void initAll() throws IOException {
        IUpdater listener = mock(IUpdater.class);
        updater = new Updater(listener, 0, "", new ArrayList<Product>(), new ArrayList<Warehouse>());
    }

    @Test
    void compareProducts(){
        Product product1 = new Product(1, "Жижка", "http://someURL", 500, new Group("Some Group", "someUrl"), 0, 120, 2.0d, "");
        Product product2 = new Product(1, "Жижка Шишка", "http://someURL", 550, new Group("Some other Group", "someUrl"), 2, 125, 3.0d, "");
        boolean result = updater.compareProducts(product1, product2);

        assertEquals(true, result);
    }
}