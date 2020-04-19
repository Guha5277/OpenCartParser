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

    private final ArrayList<Category> categories;

    Parser() {
        categories = parseCategories(fetchCategory(downloadPage()));
    }

    ArrayList<Category> getCategories() {
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

    private ArrayList<Category> parseCategories(Elements liquidPagePart) {
        ArrayList<Category> resultList = new ArrayList<>();
        liquidPagePart.forEach(catElem -> {
            Element category = catElem.child(1).select("a").get(0);
            resultList.add(new Category(category.text(), category.attr("href")));
        });
        return resultList;
    }

    private void parseGroups() {
        for (Category cat : categories) {
            try {
                //Получение групп жидкостей из категории
                Elements groupElements = Jsoup.connect(cat.getUrl()).get().body().getElementsByClass(CATEGORY_DELIMITER);

                //Перебор всех групп
                groupElements.forEach(singleGroupElement -> {
                    Group resultGroup = getGroupContains(singleGroupElement.child(1).select("a").get(0), null, cat);
                    if (resultGroup != null) cat.addGroup(resultGroup);

                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Group getGroupContains(Element element, Group parentGroup, Category category) {
        String name = element.text();
        String url = element.attr("href");
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
                Liquid resultLiq = parseLiquid(element1Liq.attr("href"), resultGroup, category.getCategoryID());
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

    private Liquid parseLiquid(String url, Group group, int categoryID) {
        try {
            Document liqPage = Jsoup.connect(url).get();
            Elements nameElement = liqPage.getElementsByClass("mobile_h1_hide");
            String name = nameElement.get(0).text();

            Elements priceElement = liqPage.getElementsByClass("price");
            String price = priceElement.get(0).text();
            int cutIndex = price.indexOf("р");
            price = price.substring(0, cutIndex);

            return new Liquid(name, url, Integer.parseInt(price), group, categoryID);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void insertAllCategories(){
        categories.forEach(SQLClient::insertCategory);
    }

    private void getCategoriesID(){
        categories.forEach(category -> {
            int id = SQLClient.getCategoryID(category);
            category.setCategoryID(id);
            System.out.println(category.getName() + " " + category.getCategoryID());
        });
    }
}
