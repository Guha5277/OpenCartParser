package main;

import main.product.Product;
import main.product.Warehouse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

class Updater extends Parser implements Runnable {
    private final ParserEvents listener;
    private int current;
    private boolean isInterrupt;
    private int updates;
    private int errors;
    private int startPosition;
    private final String IMAGES_PATH;

    Updater(ParserEvents listener, int startPosition, String imagesPath) {
        this.listener = listener;
        this.startPosition = startPosition;
        this.IMAGES_PATH = imagesPath;
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
            if (compareProducts(actualProduct, currProduct)) totalUpdated++;
            updateProductRemains(warehousesList, currProduct);
            checkProductImage(currProduct);
            current++;
        }

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

    private void checkProductImage(Product product) {
        LOG.info("Check image for product " + product.getName());
        String imageID = product.getImageID();
        if (imageID == null) {
            LOG.info("No imageID in the DB!");
            imageID = downloadImage(product);
            updateImageID(product, imageID);
        } else if (imageID.equals("NO_IMAGE")) {
            imageID = downloadImage(product);
            if (!imageID.equals("NO_IMAGE")){
                updateImageID(product, imageID);
            }
        } else {
            File file = new File(IMAGES_PATH + imageID);
            if (file.exists()) {
                LOG.info("It is not necessary to update an image");
                return;
            }
            LOG.info("No imageID in the HW!");
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
        SQLClient.updateImageID(product.getId(), imageID);
    }

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

    public void stop() {
        isInterrupt = true;
    }
}
