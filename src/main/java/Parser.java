import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

class Parser {
    private final String URL = "https://ilfumoshop.ru/zhidkost-dlya-zapravki-vejporov.html";
    private final String CATEGORY_DELIMITER = "col-lg-4 col-md-4 col-sm-6 col-xs-12";

    private Document fullPage;
    private Elements unparseCategoryPage;
    private final ArrayList<Category> categories;

    Parser() {
        fullPage = downloadPage();
        unparseCategoryPage = fetchCategory();
        categories = parseCategories();
    }

    private Document downloadPage() {
        try {
            fullPage = Jsoup.connect(URL).get();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return fullPage;
    }

    Document getFullPage() {
        return fullPage;
    }

    Elements getCategoriesPage() {
        return unparseCategoryPage;
    }

    private Elements fetchCategory() {
        return fullPage.body().getElementsByClass(CATEGORY_DELIMITER);
    }

    ArrayList<Category> parseCategories() {
        ArrayList<Category> resultList = new ArrayList<>();
        unparseCategoryPage.forEach(catElem -> {
            Element category = catElem.child(1).select("a").get(0);
            resultList.add(new Category(category.text(), category.attr("href")));
        });
        return resultList;
    }
}
