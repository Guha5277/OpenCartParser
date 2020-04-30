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

class Parser {
    static final Logger LOG = LogManager.getLogger();

    private final String URL = "https://ilfumoshop.ru/zhidkost-dlya-zapravki-vejporov.html";
    private final String CATEGORY_DELIMITER = "col-lg-4 col-md-4 col-sm-6 col-xs-12";

    Document downloadPage(String url) throws IOException{
            return  Jsoup.connect(url).get();
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
            if(categoryID == 0){
                groupName = elements.get(2).text();
                categoryName = elements.get(3).text();
                categoryID = SQLClient.getCategoryID(categoryName);

            }
//            else {
//                //categoryName = elements.get(3).text();
//            }

            price = parseStringPriceToInt(document.getElementsByClass("price").text());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return new Product(name, url, price, new Group(groupName, null), categoryID);
    }

    private int parseStringPriceToInt(String price) {
        int cutIndex = price.indexOf("р");
        return Integer.parseInt(price.substring(0, cutIndex));
    }
}