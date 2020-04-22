import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

class Parser {
    private final String URL = "https://ilfumoshop.ru/zhidkost-dlya-zapravki-vejporov.html";
    private final String CATEGORY_DELIMITER = "col-lg-4 col-md-4 col-sm-6 col-xs-12";
    private final String LIQUIDS_DELIMITER = "product-layout product-list col-xs-12";
    private static final String INNER_LIQUID_DELIMITER = "product-title";
    private static Logger LOG;

    private ArrayList<Category> categories;

    Parser() {
        LOG = LogManager.getLogger();
//        initializeCategories();
//         insertAllStores();
//        initializeDB();
    }

    ArrayList<Category> getCategories() {
        return categories;
    }

    private Document downloadPage() {
        Document result = null;
        try {
            LOG.info("Downloading Page: " + URL);
            result = Jsoup.connect(URL).get();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private Elements fetchCategory(Document liquidPage) {
        LOG.info("Getting sub-elements of page...");
        return liquidPage.body().getElementsByClass(CATEGORY_DELIMITER);
    }

    private ArrayList<Category> parseCategories(Elements liquidPagePart) {
        LOG.info("Getting categories...");
        ArrayList<Category> resultList = new ArrayList<>();
        liquidPagePart.forEach(catElem -> {
            Element category = catElem.child(1).select("a").get(0);
            String name = category.text();
            String url = category.attr("href");
            LOG.info("Parsed: " + name + " " + url);
            resultList.add(new Category(name, url));
        });
        return resultList;
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

    private void insertAllCategories(){
        categories.forEach(SQLClient::insertCategory);
    }

    private void insertAllLiquids(){
        long currentTime = System.currentTimeMillis();
        categories.forEach(category -> {
           if(category.getGroups() != null) category.getGroups().forEach(this::innerGroups);
        });
        LOG.info("Finished with " + (System.currentTimeMillis() - currentTime) + "ms");
    }

    private void innerGroups(Group group){
        if (group.isGroupHaveLiquids()) group.getProducts().forEach(SQLClient::insertNewProduct);
        if (group.isGroupHaveChild()) group.getChildGroups().forEach(this::innerGroups);
    }

    private Group getGroupContains(Element element, Group parentGroup, Category category) {
        String name = element.text();
        if (parentGroup != null){
            LOG.info("\t" + name);
        } else {
            LOG.info(name);
        }

        String url = element.attr("href");

        if(parentGroup != null && url.equals(parentGroup.getGroupURL())){
            LOG.debug("Recursive group (parent and child URL's are equals)! " +  url);
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

        Elements innerLiquids = getInnerLiquidsCount(url);
        if (innerLiquids != null && innerLiquids.size() > 0) {
            for (Element element1Liq : innerLiquids) {
                Product resultLiq = parseLiquid(element1Liq.attr("href"), resultGroup, category.getCategoryID());
                if (resultLiq == null) continue;
                    resultLiq.setGroup(resultGroup);
                    resultGroup.addLiquid(resultLiq);

            }
        }

        return resultGroup.isGroupEmpty() ? null : resultGroup;
    }

    private Elements getInnerGroups(String url) {
        try {
            return Jsoup.connect(url).get().body().getElementsByClass(CATEGORY_DELIMITER);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Elements getInnerLiquidsCount(String url) {
        try {
            return Jsoup.connect(url).get().body().getElementsByClass(INNER_LIQUID_DELIMITER);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Product parseLiquid(String url, Group group, int categoryID) {
        try {
            Document liqPage = Jsoup.connect(url).get();
            Elements nameElement = liqPage.getElementsByClass("mobile_h1_hide");
            if(nameElement.size() == 0) {
                LOG.debug("Wrong product page!(Group: " + group.getGroupName() + ", CategoryID: " + categoryID + "): " + url);
                return null;
            }
            String name = nameElement.get(0).text();

            LOG.info("\t\t" + name);

            Elements priceElement = liqPage.getElementsByClass("price");
            String price = priceElement.get(0).text();
            int cutIndex = price.indexOf("р");
            price = price.substring(0, cutIndex);

            return new Product(name, url, Integer.parseInt(price), group, categoryID);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void getCategoriesID(){
        LOG.info("Getting ID's for categories...");
        categories.forEach(category -> {
            int id = SQLClient.getCategoryID(category);
            category.setCategoryID(id);
        });
    }

    private void initializeCategories(){
        categories = parseCategories(fetchCategory(downloadPage()));
    }

    private void insertAllStores(){
        try {
            Document page = Jsoup.connect("https://ilfumoshop.ru/magaziny").get();
            Elements storesListPart = page.body().getElementsByClass("col-sm-12");
            Elements storesFetchedByCity = storesListPart.select("a");
            SQLClient.connect();
            storesPageParser(storesFetchedByCity);
            SQLClient.commit();
            SQLClient.disconnect();

        } catch (IOException e) {
            LOG.error("Error to get the stores page!");
        }
    }

    private void storesPageParser(Elements elements){
        int region = 0;
        String city;
        String address;
        String phone;

        int index = 0;
        while (index < elements.size()){
            Element storePart = elements.get(index);
            String text = storePart.text();
            int indexOfChar = text.indexOf(',');

            if (indexOfChar != -1){
                city = text.substring(0, indexOfChar);
                address = text.substring(indexOfChar + 2);
                String phonePart = elements.get(index).attr("href");

                while (!phonePart.contains("tel")){
                    index++;
                    phonePart = elements.get(index).attr("href");
                }
                indexOfChar = phonePart.indexOf('+');

                if (indexOfChar != -1 ) {
                    phone = phonePart.substring(indexOfChar);
                } else {
                    phone = phonePart;
                }

                switch (city){
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
                SQLClient.insertNewStore(region, city, address, phone);
            }
            index++;
        }
    }

    private void initializeDB(){
        SQLClient.connect();

        initializeCategories();
        insertAllCategories();
        getCategoriesID();
        insertAllStores();
        parseAllGroups();
        insertAllLiquids();

        SQLClient.commit();
        SQLClient.disconnect();
    }
}
