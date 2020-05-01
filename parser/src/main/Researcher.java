import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import product.Category;
import product.Product;

import java.io.IOException;
import java.util.ArrayList;

public class Researcher extends Parser implements Runnable {
    private final String URL;
    private final ParserEvents listener;
    private int totalInsertCount;
    private int current = 1;

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
        ArrayList<Element> list;
        for (Category category : categories) {
            list = new ArrayList<>(getCategoryElements(category));
            list.forEach(this::getGroupContent);
        }
    }

    private void checkProduct(String url) {
        if (!isProductAlreadyInDB(url)) {
            Product product = parseProduct(url);
            LOG.info("New product found: " + product.getName());
            addProduct(product);
            totalInsertCount++;
        }
    }

    private boolean isProductAlreadyInDB(String url) {
        return SQLClient.isProductAlreadyInDB(url);
    }

    private void addProduct(Product product) {
        insertProductToDB(product);
    }

    private void getGroupContent(Element element) {
        String url = element.attr("href");

        Elements innerGroups = getInnerGroups(url);
        if (innerGroups != null && innerGroups.size() > 0) {
            for (Element groupElement : innerGroups) {
                getGroupContent(groupElement.child(1).select("a").get(0));
            }
        }
        Elements innerProducts = getInnerLiquids(url);
        if (innerProducts != null && innerProducts.size() > 0) {
            for (Element productElement : innerProducts) {
                LOG.info("current: " + current++);
                checkProduct(productElement.attr("href"));
            }
        }
    }
}
