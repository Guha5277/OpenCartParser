import org.jsoup.select.Elements;
import product.Group;
import product.Product;
import product.Warehouse;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

class Updater extends Parser implements Runnable {
    private final ParserEvents listener;
    private int current = 1;
    private boolean isInterrupt;

    Updater(ParserEvents listener) {
        this.listener = listener;
        listener.onUpdaterReady();
    }

    @Override
    public void run() {
        ArrayList<Product> products = getProductsListFromDB();
        if (products == null) {
            listener.onUpdateError();
            return;
        }
        int totalUpdated = updateProductsInfo(products);
        listener.onUpdateSuccessfulEnd(current, totalUpdated);
    }

    private int updateProductsInfo(ArrayList<Product> products) {
        int overall = products.size();
        listener.onUpdaterTotalProducts(overall);
        int totalUpdated = 0;

        ArrayList<Warehouse> warehousesList = getWarehousesFromDB();

        for (Product product : products) {
            if (isInterrupt) return totalUpdated;
            listener.onUpdaterCurrentProduct(current);
            long time = System.currentTimeMillis();
            String URL = product.getURL();
            Product actualProduct = parseProduct(URL);
            if(actualProduct == null) {
                listener.onUpdateProductFailed(URL);
                current++;
                continue;
            }
            if (compareProducts(actualProduct, product)) totalUpdated++;
            updateProductRemains(warehousesList, product);
            time = System.currentTimeMillis() - time;
            LOG.info("Product updated(" + time + "): " + current++ + "/" + overall);
        }

        return totalUpdated;
    }

    private boolean compareProducts(Product actualProduct, Product oldProduct) {
        int id = oldProduct.getId();
        long time = System.currentTimeMillis();
        boolean result = false;
        String actualName = actualProduct.getName();
        String oldName = oldProduct.getName();
        String actualGroupName = actualProduct.getGroup().getName();
        String oldGroupName = oldProduct.getGroup().getName();
        int actualCategoryId = actualProduct.getCategoryID();
        int oldCategoryId = oldProduct.getCategoryID();
        int actualPrice = actualProduct.getPrice();
        int oldPrice = oldProduct.getPrice();
        int actualVolume = actualProduct.getVolume();
        int oldVolume = oldProduct.getVolume();
        double actualStrength = actualProduct.getStrength();
        double oldStrength = oldProduct.getStrength();


        if (!(actualName.equals(oldName))) {
            LOG.info("\t\tDifferences of Names!: (actual)" + actualName + " <-> " + "(old)" + oldName);
            result = true;
            SQLClient.updateProductName(id, actualName);
        }
        if (!(actualGroupName.equals(oldGroupName))) {
            LOG.info("\t\tdifferences of Groups!: (actual)" + actualGroupName + " <-> " + "(old)" + oldGroupName);
            result = true;
            SQLClient.updateProductGroupName(id, actualGroupName);
        }
        if (actualCategoryId != oldCategoryId) {
            LOG.info("\t\tdifferences of Categories!: (actual)" + actualCategoryId + " <-> " + "(old)" + oldCategoryId);
            result = true;
            SQLClient.updateProductCategory(id, actualCategoryId);
        }
        if (actualPrice != oldPrice) {
            LOG.info("\t\tdifferences of Price!: (actual)" + actualPrice + " <-> " + "(old)" + oldPrice);
            result = true;
            SQLClient.updateProductPrice(id, actualPrice);
        }
        if (actualVolume != oldVolume) {
            LOG.info("\t\tdifferences of Volume!: (actual)" + actualVolume + " <-> " + "(old)" + oldVolume);
            result = true;
            SQLClient.updateProductVolume(id, actualVolume);
        }
        if (actualStrength != oldStrength) {
            LOG.info("\t\tdifferences of Strength!: (actual)" + actualVolume + " <-> " + "(old)" + oldVolume);
            result = true;
            SQLClient.updateProductStrength(id, actualStrength);
        }
        time = System.currentTimeMillis() - time;
        return result;
    }

    private ArrayList<Product> getProductsListFromDB() {
        ArrayList<Product> list = new ArrayList<>();
        ResultSet set = SQLClient.getAllProducts();
        if (set != null) {
            try {
                while (set.next()) {
                    int id = set.getInt(1);
                    String name = set.getString(2);
                    String url = set.getString(3);
                    int price = set.getInt(4);
                    int category = set.getInt(5);
                    String groupName = set.getString(6);
                    double strength = set.getDouble(7);
                    int volume = set.getInt(8);

                    list.add(new Product(id, name, url, price, new Group(groupName, ""), category, volume, strength));
                }
            } catch (SQLException e) {
                LOG.error("Error to parse a ResultSet from DB query");
                listener.onParserException(e);
            }
        } else {
            listener.onUpdateError();
            return null;
        }
        return list;
    }

    private ArrayList<Warehouse> getWarehousesFromDB() {
        ArrayList<Warehouse> warehousesList = new ArrayList<>();

        ResultSet set = SQLClient.getAllWarehouses();
        try {
            if (set != null) {
                while (set.next()) {
                    warehousesList.add(new Warehouse(set.getInt("id"), set.getString("alt_name")));
                }
            } else {
                listener.onUpdateError();
                return null;
            }
        } catch (SQLException e) {
            LOG.error("Failed to get Warehouses List");
            listener.onParserException(e);
        }
        return warehousesList;
    }

    private void updateProductRemains(ArrayList<Warehouse> warehousesList, Product product) {
        ArrayList<Warehouse> warehouses = new ArrayList<>(warehousesList);
        Elements remainsElements;
        try {
            remainsElements = downloadPage(product.getURL()).body().getElementsByClass("tab-pane active").get(0).select("span");
        } catch (IOException e) {
            LOG.error(e);
            listener.onUpdateError();
            return;
        }


        remainsElements.forEach(element -> {
            String warehouseName = element.text();
            int index = warehouseName.indexOf(':');
            warehouseName = warehouseName.substring(0, index - 1);

            int remains = Integer.parseInt(element.child(0).text());

            for (int i = 0; i < warehouses.size(); i++) {
                if (warehouses.get(i).getAltName().equals(warehouseName)) {
                    SQLClient.updateProductRemains(warehouses.get(i).getId(), product.getId(), remains);
                    warehouses.remove(i);
                    break;
                }
            }
        });

        //updateDBtoOutOfStock
        for (Warehouse warehouse : warehouses) {
            SQLClient.updateProductRemains(warehouse.getId(), product.getId(), 0);
        }
    }

    public void stop(){
        isInterrupt = true;
    }
}
