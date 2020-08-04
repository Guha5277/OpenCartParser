package com.guhar4k.parser;

import com.guhar4k.product.Product;
import com.guhar4k.product.Warehouse;

import java.util.List;

public interface IUpdater extends IParser{
    //updater
    void onUpdaterReady();
    void onUpdateProductFailed(String url, int errorsCount);
    void onUpdaterCurrentProduct(int position, String name);
    void onUpdaterTotalProducts(int count);
    void onUpdateDiffsFound(int count, String differences);
    void onUpdateSuccessfulEnd(int checked, int updated, int errors);
    void onUpdateError();
    void onUpdaterException(int id, String url, Exception e);

//    List<Product> getAllProducts();
//    List<Warehouse> getAllWarehouses();

    void updateProductName(int id, String actualName);
    void updateProductGroupName(int id, String actualGroupName);
    void updateProductCategory(int id, int actualCategoryId);

    void updateProductPrice(int id, int actualPrice);

    void updateProductVolume(int id, int actualVolume);

    void updateProductStrength(int id, double actualStrength);

    void updateImageID(int id, String imageID);

    void updateProductRemains(int warehouseID, int productID, int remains);
}
