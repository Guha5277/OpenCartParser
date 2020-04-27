import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

class ParserOLD {
    private final String URL = "https://ilfumoshop.ru/zhidkost-dlya-zapravki-vejporov.html";
    private final String CATEGORY_DELIMITER = "col-lg-4 col-md-4 col-sm-6 col-xs-12";
    //private final String LIQUIDS_DELIMITER = "product-layout product-list col-xs-12";
    private static final String INNER_LIQUID_DELIMITER = "product-title";
    private static Logger LOG;

    private ArrayList<Category> categories;

    ParserOLD() {
        LOG = LogManager.getLogger();
        SQLClient.connect();
        initializeCategories();
        getCategoriesID();
//        updateAllProductInfo();
//        SQLClient.commit();
        SQLClient.disconnect();
    }

    //Initialize DB with data from web store
    private void initializeDB() {
        SQLClient.connect();

        initializeCategories();
        //insertAllCategories();
        getCategoriesID();
        insertAllStores();
        parseAllGroups();
        insertAllLiquids();

        SQLClient.commit();
        SQLClient.disconnect();
    }

    private void initializeCategories() {
        categories = parseCategories(downloadPage(URL));
    }

    private void insertAllCategories() {
        categories.forEach(category -> {
            SQLClient.insertCategory(category.getName());
        });
    }

    private void getCategoriesID() {
        LOG.info("Getting ID's for categories...");
        categories.forEach(category -> {
            int id = SQLClient.getCategoryID(category.getName());
            category.setCategoryID(id);
        });
    }

    private void insertAllStores() {
        try {
            Document page = Jsoup.connect("https://ilfumoshop.ru/magaziny").get();
            Elements storesListPart = page.body().getElementsByClass("col-sm-12");
            Elements storesFetchedByCity = storesListPart.select("a");
            SQLClient.connect();
            warehousesPageParser(storesFetchedByCity);
            SQLClient.commit();
            SQLClient.disconnect();

        } catch (IOException e) {
            LOG.error("Error to get the stores page!");
        }
    }

    private void parseAllGroups() {
        LOG.info("Parse all groups...");
        long currentTime = System.currentTimeMillis();
        categories.forEach(cat -> {
            try {
                //Получение групп жидкостей из категории
                LOG.info("Group: " + cat.getName());
                Elements groupElements = Jsoup.connect(cat.getUrl()).get().body().getElementsByClass(CATEGORY_DELIMITER);

                //Перебор всех групп
                groupElements.forEach(singleGroupElement -> {
                    Group resultGroup = getGroupContains(singleGroupElement.child(1).select("a").get(0), null, cat);
                    if (resultGroup != null) cat.addGroup(resultGroup);

                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        LOG.info("Finished with " + (System.currentTimeMillis() - currentTime) + "ms");
    }

    private void insertAllLiquids() {
        long currentTime = System.currentTimeMillis();
        categories.forEach(category -> {
            if (category.getGroups() != null) category.getGroups().forEach(this::innerGroups);
        });
        LOG.info("Finished with " + (System.currentTimeMillis() - currentTime) + "ms");
    }

    //Update Products Info
    private void updateAllProductInfo() {
        LOG.info("UpdateAllProducts Start");
        ArrayList<Product> products = getProductsListFromDB();
        int count = products.size();
        LOG.info("Total liquids: " + count);
        final int[] current = {0};
        ArrayList<Warehouse> warehousesList = getWarehousesFromDB();
        products.forEach(product -> {
            compareAndUpdateProductInfo(product);
            updateProductRemains(warehousesList, product);
            current[0]++;
            LOG.info("Done: " + current[0] + "/" + count);
        });

    }

    //Utils
    private void innerGroups(Group group) {
        if (group.isGroupHaveLiquids()) group.getProducts().forEach(product -> {
            SQLClient.insertProduct(product.getName(), product.getURL(), product.getPrice(), product.getCategoryID(), product.getGroup().getGroupName());
        });
        if (group.isGroupHaveChild()) group.getChildGroups().forEach(this::innerGroups);
    }

    private Elements getInnerGroups(String url) {
        try {
            return Jsoup.connect(url).get().body().getElementsByClass(CATEGORY_DELIMITER);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Elements getInnerLiquids(String url) {
        try {
            return Jsoup.connect(url).get().body().getElementsByClass(INNER_LIQUID_DELIMITER);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Group getGroupContains(Element element, Group parentGroup, Category category) {
        String name = element.text();
        if (parentGroup != null) {
            LOG.info("\t" + name);
        } else {
            LOG.info(name);
        }

        String url = element.attr("href");

        if (parentGroup != null && url.equals(parentGroup.getGroupURL())) {
            LOG.debug("Recursive group (parent and child URL's are equals)! " + url);
            return null;
        }

        Group resultGroup = new Group(name, url, parentGroup);

        Elements innerGroups = getInnerGroups(url);
        if (innerGroups != null && innerGroups.size() > 0) {
            for (Element elementG : innerGroups) {
                Group innerGroup = getGroupContains(elementG.child(1).select("a").get(0), resultGroup, category);
                if (innerGroup == null) continue;
                resultGroup.addChild(innerGroup);
            }
        }

        Elements innerLiquids = getInnerLiquids(url);
        if (innerLiquids != null && innerLiquids.size() > 0) {
            for (Element element1Liq : innerLiquids) {
                Product resultLiq = parseProduct(element1Liq.attr("href"));
                if (resultLiq == null) continue;
                resultLiq.setGroup(resultGroup);
                resultLiq.setCategoryID(category.getCategoryID());
                resultGroup.addLiquid(resultLiq);
            }
        }

        return resultGroup.isGroupEmpty() ? null : resultGroup;
    }

    private Product parseProduct(String url) {
        String name;
        String groupName;
        String categoryName;
        int price;

        try {
            Document document = Jsoup.connect(url).get();
            Elements elements = document.body().getElementsByClass("breadcrumb").select("li");
            if (elements.size() == 0) {
                System.out.println("Wrong product page!: " + url);
                return null;
            }
            name = elements.get(elements.size() - 1).text();
            groupName = elements.get(elements.size() - 2).text();
            categoryName = elements.get(2).text();
            price = parseStringPriceToInt(document.getElementsByClass("price").text());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return new Product(name, url, price, new Group(groupName, null), categoryName);
    }

    private void warehousesPageParser(Elements elements) {
        int region = 0;
        String city;
        String address;
        String phone;

        int index = 0;
        while (index < elements.size()) {
            Element storePart = elements.get(index);
            String text = storePart.text();
            int indexOfChar = text.indexOf(',');

            if (indexOfChar != -1) {
                city = text.substring(0, indexOfChar);
                address = text.substring(indexOfChar + 2);
                String phonePart = elements.get(index).attr("href");

                while (!phonePart.contains("tel")) {
                    index++;
                    phonePart = elements.get(index).attr("href");
                }
                indexOfChar = phonePart.indexOf('+');

                if (indexOfChar != -1) {
                    phone = phonePart.substring(indexOfChar);
                } else {
                    phone = phonePart;
                }

                switch (city) {
                    case "Москва":
                    case "Московская область":
                        region = 1;
                        break;
                    case "Новосибирск":
                    case "Новосибирская обл":
                    case "Бердск":
                        region = 2;
                        break;
                    case "Омск":
                        region = 3;
                        break;
                    case "Красноярск":
                        region = 4;
                        break;
                    case "Калининград":
                        region = 5;
                        break;
                }
                SQLClient.insertStore(region, city, address, phone);
            }
            index++;
        }
    }

    ArrayList<Category> getCategories() {
        return categories;
    }

    private Document downloadPage(String url) {
        Document result = null;
        try {
            LOG.info("Downloading Page: " + url);
            result = Jsoup.connect(url).get();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private ArrayList<Category> parseCategories(Document categoriesPage) {
        LOG.info("Getting categories...");
        Elements categoryParts = categoriesPage.body().getElementsByClass(CATEGORY_DELIMITER);
        ArrayList<Category> resultList = new ArrayList<>();
        categoryParts.forEach(catElem -> {
            Element category = catElem.child(1).select("a").get(0);
            String name = category.text();
            String url = category.attr("href");
            LOG.info("Parsed: " + name + " " + url);
            resultList.add(new Category(name, url));
        });
        return resultList;
    }

    private int parseStringPriceToInt(String price) {
        int cutIndex = price.indexOf("р");
        return Integer.parseInt(price.substring(0, cutIndex));
    }

    private void compareAndUpdateProductInfo(Product product) {
        int id = product.getId();
        String expectedProductName = product.getName();
        String expectedGroupName = product.getGroup().getGroupName();
        int expectedPrice = product.getPrice();
        int expectedCategoryId = product.getCategoryID();

        String actualProductName;
        String actualGroupName;
        String actualCategory;
        int actualPrice;
        int actualCategoryId;

        LOG.info("\tUpdate info for: " + product.getName());

        try {
            Document document = Jsoup.connect(product.getURL()).get();
            Elements elements = document.body().getElementsByClass("breadcrumb").select("li");
            actualProductName = elements.get(elements.size() - 1).text();
            actualGroupName = elements.get(elements.size() - 2).text();
            actualCategory = elements.get(2).text();
            actualPrice = parseStringPriceToInt(document.getElementsByClass("price").text());

            actualCategoryId = 0;
            for (; actualCategoryId < categories.size(); actualCategoryId++) {
                if (actualCategory.equals(categories.get(actualCategoryId).getName())) {
                    actualCategoryId = categories.get(actualCategoryId).getCategoryID();
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        if (!(actualProductName.equals(expectedProductName))) {
            LOG.info("\t\tdifferences of Names!: " + actualProductName + " <-> " + product.getName());
            SQLClient.updateProductName(id, actualProductName);
        }
        if (!(actualGroupName.equals(expectedGroupName))) {
            LOG.info("\t\tdifferences of Groups!: " + actualGroupName + " <-> " + product.getGroup().getGroupName());
            SQLClient.updateProductGroupName(id, actualGroupName);
        }
        if (actualCategoryId != expectedCategoryId) {
            LOG.info("\t\tdifferences of Categories!: " + actualCategoryId + " <-> " + product.getCategoryID());
            SQLClient.updateProductCategory(id, actualCategoryId);
        }
        if (actualPrice != expectedPrice) {
            LOG.info("\t\tdifferences of Price!: " + actualPrice + " <-> " + product.getPrice());
            SQLClient.updateProductPrice(id, actualPrice);
        }
    }

    private ArrayList<Warehouse> getWarehousesFromDB(){
        ArrayList<Warehouse> warehousesList = new ArrayList<>();

        ResultSet set = SQLClient.getAllWarehouses();
        try {
            assert set != null;
            while (set.next()){
                warehousesList.add(new Warehouse(set.getInt("id"), set.getString("alt_name")));
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
        return warehousesList;
    }

    private ArrayList<Product> getProductsListFromDB() {
        ArrayList<Product> list = new ArrayList<>();
        ResultSet set = SQLClient.getAllProducts();

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
        }
        return list;
    }

    private void updateProductRemains(ArrayList<Warehouse> warehousesList, Product product){
        ArrayList<Warehouse> warehouses = new ArrayList<>(warehousesList);
        LOG.info("\tUpdate remains for: " + product.getName());
        try {
            Document document = Jsoup.connect(product.getURL()).get();
            Elements remainsElements = document.body().getElementsByClass("tab-pane active").get(0).select("span");

            remainsElements.forEach(element -> {
                String warehouseName = element.text();
                int index = warehouseName.indexOf(':');
                warehouseName = warehouseName.substring(0, index-1);

                int remains = Integer.parseInt(element.child(0).text());

                for (int i = 0; i < warehouses.size(); i ++){
                    if (warehouses.get(i).getAltName().equals(warehouseName)){
                        SQLClient.updateProductRemains(warehouses.get(i).getId(), product.getId(), remains);
                        warehouses.remove(i);
                        break;
                    }
                }
            });

            //updateDBtoOutOfStock
            for (int i = 0; i < warehouses.size(); i ++){
                SQLClient.updateProductRemains(warehouses.get(i).getId(), product.getId(), 0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
