import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

class Parser {
    private final String URL = "https://ilfumoshop.ru/zhidkost-dlya-zapravki-vejporov.html";
    private final String CATEGORY_DELIMITER = "col-lg-4 col-md-4 col-sm-6 col-xs-12";

    private final ArrayList<Category> categories;

    Parser() {
        categories = parseCategories(fetchCategory(downloadPage()));
    }

    public ArrayList<Category> getCategories() {
        return categories;
    }

    private Document downloadPage() {
        Document result = null;
        try {
            result = Jsoup.connect(URL).get();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private Elements fetchCategory(Document liquidPage) {
        return liquidPage.body().getElementsByClass(CATEGORY_DELIMITER);
    }

    ArrayList<Category> parseCategories(Elements liquidPagePart) {
        ArrayList<Category> resultList = new ArrayList<>();
        liquidPagePart.forEach(catElem -> {
            Element category = catElem.child(1).select("a").get(0);
            resultList.add(new Category(category.text(), category.attr("href")));
        });
        return resultList;
    }
}
