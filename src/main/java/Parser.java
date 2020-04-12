import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class Parser {
    private final String URL = "https://ilfumoshop.ru/zhidkost-dlya-zapravki-vejporov.html";
    private final String CATEGORY_DELIMITER = "col-lg-4 col-md-4 col-sm-6 col-xs-12";
    Elements categories;
    private Document page;


    Parser() {
        page = downloadPage();
        categories = parseCategories();
    }

    public Document getPage() {
        return page;
    }

    public Elements getCategories(){
        return categories;
    }

     private Document downloadPage() {
        try{
            page = Jsoup.connect(URL).get();

        } catch (IOException e){
            e.printStackTrace();
        }
        return page;
    }

    private Elements parseCategories() {
        return page.body().getElementsByClass(CATEGORY_DELIMITER);
    }

    public void showCategories(){

        Element link = categories.select("a").first();
        String href = link.attr("href");
        System.out.println(href);

//        link = categories.select("a").next();

//
//        for (Element links : categories.select("a").next()){
//            String href = links.attr("href");
//            System.out.println(href);
//        }


//
//        int i = 0;
//        for(String s : categories.eachText()){
//            System.out.print(i + ": "); System.out.println(s);
//            i++;
//        }
    }
}
