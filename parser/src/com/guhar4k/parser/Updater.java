package com.guhar4k.parser;

import com.guhar4k.product.Product;
import com.guhar4k.product.Warehouse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Updater extends Parser implements Runnable {
    private final IUpdater listener;
    private int current;
    private boolean isInterrupt;
    private int updates;
    private int errors;
    private int startPosition;
    private final String IMAGES_PATH;
    private List<Product> productList;
    private List<Warehouse> warehouseList;

    public Updater(IUpdater listener, int startPosition, String imagesPath, List<Product> productList, List<Warehouse> warehouseList) {
        super(listener);
        this.listener = listener;
        this.startPosition = startPosition;
        this.IMAGES_PATH = imagesPath;
        this.productList = productList;
        this.warehouseList = warehouseList;
        current = startPosition + 1;
        listener.onUpdaterReady();
        LOG.info("Updater Instance Created");
    }

    @Override
    public void run() {
        LOG.info("Updater working...");
        if (productList == null) {
            listener.onUpdateError();
            return;
        }
        int totalUpdated = updateProductsInfo(productList);
        listener.onUpdateSuccessfulEnd(current, totalUpdated, errors);
        LOG.info("Updater finished");
    }

    private int updateProductsInfo(List<Product> products) {
        int overall = products.size();
        listener.onUpdaterTotalProducts(overall);
        int totalUpdated = 0;

        for (int i = startPosition; i < products.size(); i++) {
            Product currProduct = products.get(i);
            if (isInterrupt) return totalUpdated;
            listener.onUpdaterCurrentProduct(current, currProduct.getName());
            String URL = currProduct.getURL();
            Product actualProduct = null;
            try {
                actualProduct = parseProduct(URL);
            } catch (IOException e) {
                listener.onUpdaterException(currProduct.getId(), URL, e);
            }
            if (actualProduct == null) {
                errors++;
                current++;
                listener.onUpdateProductFailed(URL, errors);
                continue;
            }
            if (compareProduct(actualProduct, currProduct)) totalUpdated++;
            updateProductRemains(warehouseList, currProduct);
            checkProductImage(currProduct);
            current++;
        }

        return totalUpdated;
    }

    boolean compareProduct(Product actualProduct, Product oldProduct) {
        boolean result = false;
        StringBuilder diffBuild = new StringBuilder();
        int id = oldProduct.getId();
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
            diffBuild.append(makeDiffString("name", oldName, actualName));
            listener.updateProductName(id, actualName);
        }

        if (!(actualGroupName.equals(oldGroupName))) {
            LOG.info("\t\tdifferences of Groups!: (actual)" + actualGroupName + " <-> " + "(old)" + oldGroupName);
            result = true;
            diffBuild.append(makeDiffString("groupName", oldGroupName, actualGroupName));
            listener.updateProductGroupName(id, actualGroupName);
        }
        if (actualCategoryId != oldCategoryId) {
            LOG.info("\t\tdifferences of Categories!: (actual)" + actualCategoryId + " <-> " + "(old)" + oldCategoryId);
            result = true;
            diffBuild.append(makeDiffString("categoryID", oldCategoryId, actualCategoryId));
            listener.updateProductCategory(id, actualCategoryId);
        }
        if (actualPrice != oldPrice) {
            LOG.info("\t\tdifferences of Price!: (actual)" + actualPrice + " <-> " + "(old)" + oldPrice);
            result = true;
            diffBuild.append(makeDiffString("price", oldPrice, actualPrice));
            listener.updateProductPrice(id, actualPrice);
        }
        if (actualVolume != oldVolume) {
            LOG.info("\t\tdifferences of Volume!: (actual)" + actualVolume + " <-> " + "(old)" + oldVolume);
            result = true;
            diffBuild.append(makeDiffString("volume", oldVolume, actualVolume));
            listener.updateProductVolume(id, actualVolume);
        }
        if (actualStrength != oldStrength) {
            LOG.info("\t\tdifferences of Strength!: (actual)" + actualStrength + " <-> " + "(old)" + oldStrength);
            result = true;
            diffBuild.append(makeDiffString("strength", oldStrength, actualStrength));
            listener.updateProductStrength(id, actualStrength);
        }

        if (result) {
            updates++;
            listener.onUpdateDiffsFound(updates, diffBuild.toString());
        }
        return result;
    }

    private String makeDiffString(String type, String oldValue, String actualValue) {
        return type + ": " + oldValue + " ---> " + actualValue + "\n";
    }

    private String makeDiffString(String type, int oldValue, int actualValue) {
        return type + ": " + oldValue + " ---> " + actualValue + "\n";
    }

    private String makeDiffString(String type, double oldValue, double actualValue) {
        return type + ": " + oldValue + " ---> " + actualValue + "\n";
    }

    private void checkProductImage(Product product) {
        LOG.info("Checking image...");
        String imageID = product.getImageID();
        if (imageID == null) {
            LOG.info("No imageID in the DB!");
            imageID = downloadImage(product);
            updateImageID(product, imageID);
        } else if (imageID.equals("NO_IMAGE")) {
            imageID = downloadImage(product);
            if (!imageID.equals("NO_IMAGE")) {
                updateImageID(product, imageID);
            }
        } else {
            File file = new File(IMAGES_PATH + imageID);
            if (file.exists()) return;

            imageID = downloadImage(product);
            updateImageID(product, imageID);
        }
    }

    private String downloadImage(Product product) {
        LOG.info("Downloading image...");
        String URL = product.getURL();
        String name = null;
        Document doc;
        try {
            doc = Jsoup.connect(URL).get();
            //Получение картинки
            Element img;
            try {
                img = doc.body().getElementsByClass("thumbnail").get(0);
            } catch (IndexOutOfBoundsException e) {
                LOG.error("The product has no image!" + URL);
                return "NO_IMAGE";
            }

            String url = img.absUrl("href");
            System.out.println("URL: " + url);

            //Получение индекса имени картинки
            int indexname = url.lastIndexOf("/");

            //Получение имени картинки
            name = url.substring(indexname + 1);
            System.out.println("Image name: " + name);

            //Open a URL Stream
            java.net.URL urlStream = new URL(url);
            InputStream in = urlStream.openStream();

            OutputStream out = new BufferedOutputStream(new FileOutputStream(IMAGES_PATH + name));

            int size = in.available();
            for (int b; (b = in.read()) != -1; ) {
                out.write(b);
            }
            out.close();
            in.close();
            LOG.info("Image successful download and write to disk: " + IMAGES_PATH + name + " file size: " + size);
        } catch (IOException e) {
            LOG.error("Error when trying to ge an product image " + e.getMessage());
        }
        return name;
    }

    private void updateImageID(Product product, String imageID) {
        LOG.info("Updating image to DB...");
        listener.updateImageID(product.getId(), imageID);
    }

    private void updateProductRemains(List<Warehouse> warehousesList, Product product) {
        LOG.info("Updating remains...");
        ArrayList<Warehouse> warehouses = new ArrayList<>(warehousesList);
        Elements remainsElements;
        try {
            remainsElements = getRemainsElements(product.getURL());
        } catch (IOException | IndexOutOfBoundsException e) {
            LOG.error(e);
            listener.onUpdaterException(product.getId(), product.getURL(), e);
            return;
        }

        for (Element element : remainsElements) {
            String warehouseNameContainer = element.text();
            String warehouseName = parseWarehouseName(warehouseNameContainer);

            String remainsContainer = element.child(0).text();
            int sumIndex = remainsContainer.contains("+") ? 1 : 0;
            int remains = parseRemains(remainsContainer, sumIndex);


            int warehouseIndex = findWarehouseIndex(warehouseName, warehouses);
            if (warehouseIndex == -1) {
                LOG.error("Can't find a warehouse by name from the list!");
                continue;
            }
            listener.updateProductRemains(warehouses.get(warehouseIndex).getId(), product.getId(), remains);
            warehouses.remove(warehouseIndex);
        }

        updateOutOfStock(warehouses, product);
    }

    private Elements getRemainsElements(String url) throws IOException, IndexOutOfBoundsException {
        return downloadPage(url).body().getElementsByClass("tab-pane active").get(0).select("span");
    }

    private String parseWarehouseName(String nameContainer) {
        int index = nameContainer.indexOf(':');
        return nameContainer.substring(0, index - 1);
    }

    private int parseRemains(String remainsContainer, int sumIndex) {
        if (remainsContainer.contains("+")) {
            remainsContainer = remainsContainer.substring(0, remainsContainer.length() - 1);
        }
        return Integer.parseInt(remainsContainer) + sumIndex;
    }

    private int findWarehouseIndex(String warehouseName, List<Warehouse> warehouseList) {
        for (int i = 0; i < warehouseList.size(); i++) {
            if (warehouseList.get(i).getAltName().equals(warehouseName)) return i;
        }
        return -1;
    }

    private void updateOutOfStock(ArrayList<Warehouse> warehouses, Product product) {
        for (Warehouse warehouse : warehouses) {
            listener.updateProductRemains(warehouse.getId(), product.getId(), 0);
        }
    }


    public void stop() {
        isInterrupt = true;
    }
}
