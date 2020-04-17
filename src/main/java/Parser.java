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

    ArrayList<Category> parseCategories(Elements liquidPagePart) {
        ArrayList<Category> resultList = new ArrayList<>();
        liquidPagePart.forEach(catElem -> {
            Element category = catElem.child(1).select("a").get(0);
            resultList.add(new Category(category.text(), category.attr("href")));
        });
        return resultList;
    }

    public void parseGroups() {
        for (Category cat : categories) {
            try {
                //Получение групп жидкостей из категории
                Elements groupElements = Jsoup.connect(cat.getUrl()).get().body().getElementsByClass(CATEGORY_DELIMITER);

                //Перебор всех групп
                groupElements.forEach(singleGroupElement -> {

                    String[] groupData = getGroupContains(singleGroupElement.child(1).select("a").get(0));

                   // Element group = groupElement.child(1).select("a").get(0);

//                    String groupName = group.text();
//                    String groupUrl = group.attr("href");

                    //Проверка, содержит ли группа жидкости
                    if (!isGroupEmpty(groupUrl)) {
                        cat.addGroup(new Group(groupName, groupUrl));
                    }

                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private boolean isGroupEmpty(String url) {
        Elements groupPage = null;
        try {
            Document doc = Jsoup.connect(url).get();
            groupPage = doc.body().getElementsByClass(LIQUIDS_DELIMITER);
            if (groupPage.size() == 0) groupPage = doc.getElementsByClass(CATEGORY_DELIMITER);

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return groupPage.size() == 0;
    }

    private boolean isGroupContainGroups(String url){

    }

    private boolean isGroupContainLiquids(String url){

    }

    private String[] getGroupContains(Category category, String groupURL){
        boolean isGroupEmpty = true;
        String [] result = new String[2];

        if (isGroupContainGroups(groupElement.attr("href"))){
            isGroupEmpty = false;
        }
        if (isGroupContainLiquids()){
            isGroupEmpty = false;
        }
        return isGroupEmpty ? null : result;
    }
}
