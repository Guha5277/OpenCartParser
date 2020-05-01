import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import product.Category;
import product.Group;
import product.Product;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class Parser {
    static final Logger LOG = LogManager.getLogger();

    private final String URL = "https://ilfumoshop.ru/zhidkost-dlya-zapravki-vejporov.html";
    private final String CATEGORY_DELIMITER = "col-lg-4 col-md-4 col-sm-6 col-xs-12";
    private static final String INNER_LIQUID_DELIMITER = "product-title";

    Document downloadPage(String url) throws IOException {
        return Jsoup.connect(url).get();
    }

    ArrayList<Category> getCategories(Document categoriesPage) {
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

    Elements getInnerGroups(String url) {
        try {
            return Jsoup.connect(url).get().body().getElementsByClass(CATEGORY_DELIMITER);
        } catch (IOException e) {
            LOG.error(e);
            return null;
        }
    }

    Elements getInnerLiquids(String url) {
        try {
            return Jsoup.connect(url).get().body().getElementsByClass(INNER_LIQUID_DELIMITER);
        } catch (IOException e) {
            LOG.error(e);
            return null;
        }
    }

    void getCategoriesID(ArrayList<Category> categories) {
        LOG.info("Getting ID's for categories...");
        categories.forEach(category -> {
            int id = SQLClient.getCategoryID(category.getName());
            category.setCategoryID(id);
        });
    }

    Product parseProduct(String url) {
        String name;
        String groupName;
        String categoryName;
        int price;
        int categoryID;
        int volume;
        double strength = 0;

        try {
            Document document = Jsoup.connect(url).get();
            Elements elements = document.body().getElementsByClass("breadcrumb").select("li");
            if (elements.size() == 0) {
                System.out.println("Wrong product page!: " + url);
                return null;
            }
            name = elements.get(elements.size() - 1).text();

            categoryName = elements.get(2).text();
            groupName = elements.get(3).text();


            categoryID = SQLClient.getCategoryID(categoryName);
            if (categoryID == 0) {
                groupName = elements.get(2).text();
                categoryName = elements.get(3).text();
                categoryID = SQLClient.getCategoryID(categoryName);

            }

            price = parseStringPriceToInt(document.getElementsByClass("price").text());
            volume = parseVolume(name);
            if (volume != 0) {
                strength = parseStrength(name);
                name = trimToValidName(name, volume, strength);
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return new Product(name, url, price, new Group(groupName, null), categoryID, volume, strength);
    }

    private int parseStringPriceToInt(String price) {
        int cutIndex = price.indexOf("р");
        return Integer.parseInt(price.substring(0, cutIndex));
    }

    List<Element> getCategoryElements(Category category) {
        List<Element> groupList = new ArrayList<>();
        Elements groupElements = null;
        try {
            groupElements = downloadPage(category.getUrl()).body().getElementsByClass(CATEGORY_DELIMITER);
        } catch (IOException e) {
            LOG.error(e);
            return null;
        }

        groupElements.forEach(singleGroupElement -> {
            groupList.add(singleGroupElement.child(1).select("a").get(0));
        });

        return groupList;
    }

    int parseVolume(String name) {
        int indexEnd = name.indexOf(" мл");
        int indexStart = indexEnd - 1;
        for (; indexStart > 0; indexStart--) {
            if (name.charAt(indexStart) == 32) break;
        }

        if (name.indexOf('*') > 0){
            int mult = name.charAt(indexStart+1);
            int value = Integer.valueOf(name.substring(indexStart + 3, indexEnd));
            return value * mult;
        }

        if (indexStart < 0) return 0;
        String result = name.substring(indexStart + 1, indexEnd);
        return Integer.parseInt(result);
    }

    double parseStrength(String name) {
        int indexEnd = name.indexOf(" мг");
        if (indexEnd == -1) return 3.0d;
        int indexStart = indexEnd - 1;
        for (; indexStart > 0; indexStart--) {
            if (name.charAt(indexStart) == 32) break;
        }

        String result = name.substring(indexStart + 1, indexEnd);
        if (result.indexOf(',') > 0) result = result.replace(',', '.');
        return Double.parseDouble(result);
    }

    String trimToValidName(String name, int volume, double strength) {
        int indexStart = name.indexOf(String.valueOf(volume)) - 1;

        if (indexStart < 0) {
            indexStart = name.indexOf('*') - 2;
        }

        int indexEnd = name.indexOf(" мл") + 3;
        String firstPart = name.substring(0, indexStart);
        String secondPart = name.substring(indexEnd);



        if (strength != 1.5d) {
            if (secondPart.equals(" " + (int)strength + " мг/мл")){
                return firstPart;
            }
            indexEnd = secondPart.indexOf((String.valueOf((int)strength))) - 1;
        } else {
            if (secondPart.equals("1.5 мг/мл") || secondPart.equals("01.5 мг/мл")){
                return firstPart;
            }
            indexEnd = secondPart.indexOf("1.5") - 1;
        }
        if (indexEnd > 0) secondPart = secondPart.substring(0, indexEnd);
        return firstPart + secondPart;
    }

    void insertProductToDB(Product product){
        SQLClient.insertProduct(product.getName(), product.getURL(), product.getPrice(), product.getCategoryID(), product.getGroup().getName(), product.getVolume(), product.getStrength());
    }
}