package main;

import main.product.Product;
import main.product.Warehouse;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class Updater extends Parser implements Runnable {
    private final ParserEvents listener;
    private int current;
    private boolean isInterrupt;
    private int updates;
    private int errors;
    private int startPosition;

    Updater(ParserEvents listener, int startPosition) {
        this.listener = listener;
        this.startPosition = startPosition;
        current = startPosition + 1;
        listener.onUpdaterReady();
        LOG.info("Updater Instance Created");
    }

    @Override
    public void run() {
        LOG.info("Updater working...");
        List<Product> products = SQLClient.getAllProducts();
        if (products == null) {
            listener.onUpdateError();
            return;
        }
        int totalUpdated = updateProductsInfo(products);
        listener.onUpdateSuccessfulEnd(current, totalUpdated, errors);
        LOG.info("Researcher finished");
    }

    private int updateProductsInfo(List<Product> products) {
        int overall = products.size();
        listener.onUpdaterTotalProducts(overall);
        int totalUpdated = 0;

        List<Warehouse> warehousesList = SQLClient.getAllWarehouses();

        for (int i = startPosition; i < products.size(); i++){
            if (isInterrupt) return totalUpdated;
            listener.onUpdaterCurrentProduct(current, products.get(i).getName());
            String URL = products.get(i).getURL();
            Product actualProduct = null;
            try {
                actualProduct = parseProduct(URL);
            } catch (IOException e) {
                listener.onUpdaterException(products.get(i).getId(), URL, e);
            }
            if(actualProduct == null) {
                errors++;
                current++;
                listener.onUpdateProductFailed(URL, errors);
                continue;
            }
            if (compareProducts(actualProduct, products.get(i))) totalUpdated++;
            updateProductRemains(warehousesList, products.get(i));
            current++;
        }

//        for (Product product : products) {
//            if (isInterrupt) return totalUpdated;
//            listener.onUpdaterCurrentProduct(current, product.getName());
//            String URL = product.getURL();
//            Product actualProduct = null;
//            try {
//                actualProduct = parseProduct(URL);
//            } catch (IOException e) {
//                listener.onUpdaterException(product.getId(), URL, e);
//            }
//            if(actualProduct == null) {
//                errors++;
//                current++;
//                listener.onUpdateProductFailed(URL, errors);
//                continue;
//            }
//            if (compareProducts(actualProduct, product)) totalUpdated++;
//            updateProductRemains(warehousesList, product);
//            current++;
//        }

        return totalUpdated;
    }

    private boolean compareProducts(Product actualProduct, Product oldProduct) {
        boolean productHaveUpdate = false;
        StringBuilder diffBuild = new StringBuilder();
        int id = oldProduct.getId();
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


        diffBuild.append("Differences of product with id ");
        diffBuild.append(id);
        diffBuild.append(": \n");

        if (!(actualName.equals(oldName))) {
            LOG.info("\t\tDifferences of Names!: (actual)" + actualName + " <-> " + "(old)" + oldName);
            result = true;
            diffBuild.append("name: ");
            diffBuild.append(oldName);
            diffBuild.append(" ---> ");
            diffBuild.append(actualName);
            diffBuild.append("\n");

            SQLClient.updateProductName(id, actualName);
            productHaveUpdate = true;
        }
        if (!(actualGroupName.equals(oldGroupName))) {
            LOG.info("\t\tdifferences of Groups!: (actual)" + actualGroupName + " <-> " + "(old)" + oldGroupName);
            result = true;
            diffBuild.append("groupName: ");
            diffBuild.append(oldGroupName);
            diffBuild.append(" ---> ");
            diffBuild.append(actualGroupName);
            diffBuild.append("\n");

            SQLClient.updateProductGroupName(id, actualGroupName);
            productHaveUpdate = true;
        }
        if (actualCategoryId != oldCategoryId) {
            LOG.info("\t\tdifferences of Categories!: (actual)" + actualCategoryId + " <-> " + "(old)" + oldCategoryId);
            result = true;
            diffBuild.append("category: ");
            diffBuild.append(oldCategoryId);
            diffBuild.append(" ---> ");
            diffBuild.append(actualCategoryId);
            diffBuild.append("\n");

            SQLClient.updateProductCategory(id, actualCategoryId);
            productHaveUpdate = true;
        }
        if (actualPrice != oldPrice) {
            LOG.info("\t\tdifferences of Price!: (actual)" + actualPrice + " <-> " + "(old)" + oldPrice);
            result = true;
            diffBuild.append("price: ");
            diffBuild.append(oldPrice);
            diffBuild.append(" ---> ");
            diffBuild.append(actualPrice);
            diffBuild.append("\n");

            SQLClient.updateProductPrice(id, actualPrice);
            productHaveUpdate = true;
        }
        if (actualVolume != oldVolume) {
            LOG.info("\t\tdifferences of Volume!: (actual)" + actualVolume + " <-> " + "(old)" + oldVolume);
            result = true;
            diffBuild.append("volume: ");
            diffBuild.append(oldVolume);
            diffBuild.append(" ---> ");
            diffBuild.append(actualVolume);
            diffBuild.append("\n");

            SQLClient.updateProductVolume(id, actualVolume);
            productHaveUpdate = true;
        }
        if (actualStrength != oldStrength) {
            LOG.info("\t\tdifferences of Strength!: (actual)" + actualStrength + " <-> " + "(old)" + oldStrength);
            result = true;
            diffBuild.append("strength: ");
            diffBuild.append(oldStrength);
            diffBuild.append(" ---> ");
            diffBuild.append(actualStrength);

            SQLClient.updateProductStrength(id, actualStrength);
            productHaveUpdate = true;
        }

        if (productHaveUpdate) {
            updates++;
            listener.onUpdateDiffsFound(updates, diffBuild.toString());
        }
        return result;
    }

    private ArrayList<Product> getProductsListFromDB() {
//        ArrayList<Product> list = new ArrayList<>();
//        ResultSet set = SQLClient.getAllProducts();

        List<Product> list = SQLClient.getAllProducts();
        //list = SQLClient.getAllProducts();
//        if (set != null) {
//            try {
//                while (set.next()) {
//                    int id = set.getInt(1);
//                    String name = set.getString(2);
//                    String url = set.getString(3);
//                    int price = set.getInt(4);
//                    int category = set.getInt(5);
//                    String groupName = set.getString(6);
//                    double strength = set.getDouble(7);
//                    int volume = set.getInt(8);
//
//                    list.add(new Product(id, name, url, price, new Group(groupName, ""), category, volume, strength));
//                }
//            } catch (SQLException e) {
//                LOG.error("Error to parse a ResultSet from DB query");
//                listener.onParserException(e);
//            }
//        } else {
//            listener.onUpdateError();
//            return null;
//        }
        return null;
    }

//    private ArrayList<Warehouse> getWarehousesFromDB() {
//        ArrayList<Warehouse> warehousesList = new ArrayList<>();
//
//        ResultSet set = SQLClient.getAllWarehouses();
//        try {
//            if (set != null) {
//                while (set.next()) {
//                    warehousesList.add(new Warehouse(set.getInt("id"), set.getString("alt_name")));
//                }
//            } else {
//                listener.onUpdateError();
//                return null;
//            }
//        } catch (SQLException e) {
//            LOG.error("Failed to get Warehouses List");
//            listener.onUpdaterSQLException(e);
//        }
//        return warehousesList;
//    }

    private void updateProductRemains(List<Warehouse> warehousesList, Product product) {
        ArrayList<Warehouse> warehouses = new ArrayList<>(warehousesList);
        Elements remainsElements;
        try {
            remainsElements = downloadPage(product.getURL()).body().getElementsByClass("tab-pane active").get(0).select("span");
        } catch (IOException | IndexOutOfBoundsException e) {
            LOG.error(e);
            listener.onUpdaterException(product.getId(), product.getURL(), e);
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
