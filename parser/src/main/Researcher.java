package main;

import main.product.Category;
import main.product.Product;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Researcher extends Parser implements Runnable {
    private final String URL;
    private final ParserEvents listener;
    private int currentCategory = 1;
    private int categoryGroupsCount;
    private int currentGroup;
    private int totalInsertCount;
    private int current = 1;
    private boolean isInterrupt;

    Researcher(String url, ParserEvents listener) {
        this.URL = url;
        this.listener = listener;
        listener.onResearcherReady();
    }

    @Override
    public void run() {
        Document page;
        try {
            page = downloadPage(URL);
        } catch (IOException e) {
            LOG.error(e);
            listener.onResearchError();
            return;
        }
        ArrayList<Category> categories = getCategories(page);
        findNewProducts(categories);
        listener.onResearchSuccessfulEnd(totalInsertCount);
    }

    private void findNewProducts(ArrayList<Category> categories) {
        int categoriesCount = categories.size();
        ArrayList<Element> list;
        for (Category category : categories) {
            if (isInterrupt) return;
            List<Element> tempList = null;
            try {
                tempList = getCategoryElements(category);
            } catch (IOException e) {
                LOG.error(e.getMessage());
                listener.onResearchError();
            }
            if (tempList != null){
                categoryGroupsCount = tempList.size();
                listener.onResearcherCurrentCategory(categoriesCount, currentCategory, category.getName());
                currentGroup = 0;
                list = new ArrayList<>(tempList);
                list.forEach(this::getGroupContent);
            }
            currentCategory++;
        }
    }

    private boolean isProductAlreadyInDB(String url) {
        return SQLClient.isProductAlreadyInDB(url);
    }

    private void addProduct(Product product) {
        insertProductToDB(product);
    }

    private void getGroupContent(Element element) {
        if (isInterrupt) return;
        currentGroup++;

        String url = element.attr("href");
        Elements innerGroups = getInnerGroups(url);
        listener.onResearcherCurrentGroup(categoryGroupsCount, currentGroup, element.text());
        if (innerGroups != null && innerGroups.size() > 0) {
            categoryGroupsCount += innerGroups.size();
            for (Element groupElement : innerGroups) {
                getGroupContent(groupElement.child(1).select("a").get(0));
            }
        }
        Elements innerProducts = getInnerLiquids(url);
        if (innerProducts != null && innerProducts.size() > 0) {
            for (Element productElement : innerProducts) {
                //LOG.info("current: " + current++);
                checkProduct(productElement.attr("href"));
            }
        }
    }

    private void checkProduct(String url) {
        if (!isProductAlreadyInDB(url)) {
            Product product;
            try {
                product = parseProduct(url);
                String name = product.getName();
                LOG.info("New product found: " + name);
                addProduct(product);
                listener.onResearcherFoundNewProduct(name, totalInsertCount++);
            } catch (IOException e) {
                listener.onParserException(e);
            }
        }
    }

    void stop(){
        isInterrupt = true;
    }
}
