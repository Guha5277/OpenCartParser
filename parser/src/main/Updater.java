import org.jsoup.select.Elements;
import product.Group;
import product.Product;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

class Updater extends Parser implements Runnable {
    private final ParserEvents listener;

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
        listener.onUpdateSuccessfulEnd(totalUpdated);
    }

    private int updateProductsInfo(ArrayList<Product> products) {
        int overall = products.size();
        int totalUpdated = 0;
        int current = 1;

        ArrayList<Warehouse> warehousesList = getWarehousesFromDB();

        for (Product product : products) {
            Product actualProduct = parseProduct(product.getURL());
            if (compareProducts(actualProduct, product)) totalUpdated++;
            updateProductRemains(warehousesList, product);
            LOG.info("Done: " + current++ + "/" + overall);
        }

        return totalUpdated;
    }

    private boolean compareProducts(Product actualProduct, Product oldProduct) {
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
                    list.add(new Product(id, name, url, price, new Group(groupName, ""), category));
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

    public void test() {
        ArrayList<Product> list = getProductsListFromDB();
        System.out.println(list.size());
        File fileDone = new File("log/done.txt");
        File fileError = new File("log/error.txt");

        int doneCounter = 1;
        int errorCounter = 1;


        try {
            if (fileDone.exists()) {
                fileDone.delete();
                fileDone.createNewFile();
            }

            if (fileError.exists()) {
                fileError.delete();
                fileError.createNewFile();
            }

            FileWriter writerDone = new FileWriter(fileDone, true);
            FileWriter writerError = new FileWriter(fileError, true);

            for (Product product : list) {
                String name = product.getName();
                parseVolume(product);
                if (product.getVolume() == 0) {
                    writerError.write(errorCounter + ": " + name + "\n");
                    errorCounter++;
                } else {
                    writerDone.write(doneCounter + ": " + name + "\n");
                    writerDone.write(doneCounter + ": " + product.getName() + "\n");
                    writerDone.write("Объём: " + product.getVolume() + "\n\n");
                    doneCounter++;
                }
            }

            writerDone.close();
            writerError.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
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
        //LOG.info("\tUpdate remains for: " + product.getName());
        Elements remainsElements = null;
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
}
