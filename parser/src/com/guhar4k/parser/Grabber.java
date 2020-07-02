package com.guhar4k.parser;

import com.guhar4k.product.Category;
import com.guhar4k.product.Group;
import com.guhar4k.product.Product;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/*TODO изменить логику работы
* 1. Нет необходимости повторять иерархию сайта (не сохранять всё в памяти)
* 2. При нахождении новой жидкости отправлять её listener'y*/

class Grabber extends Parser implements Runnable {
    private final String CATEGORY_DELIMITER = "col-lg-4 col-md-4 col-sm-6 col-xs-12";

    private final String URL;
    private final IGrabber listener;
    private int totalInsertCount;

    Grabber(String url, IGrabber listener) {
        super(listener);
        this.URL = url;
        this.listener = listener;
    }

    @Override
    public void run() {
        listener.onGrabberReady();

        Document page;
        try {
            page = downloadPage(URL);
        } catch (IOException e) {
            LOG.error(e);
            listener.onGrabError();
            return;
        }
        ArrayList<Category> categories = getCategories(page);
        insertAllCategories(categories);
        setIDForCategories(categories);
        getCategoriesContent(categories);

        if (!insertAllWarehouses(getWarehousesList())) {
            listener.onGrabError();
            return;
        }
        insertAllLiquids(categories);

        listener.onGrabberSuccessfulEnd(totalInsertCount);
    }

    private void getCategoriesContent(ArrayList<Category> categories) {
        for (Category cat : categories) {
            //Получение групп жидкостей из категории
            LOG.info("product.Group: " + cat.getName());
            Elements groupElements = null;
            try {
                groupElements = downloadPage(cat.getUrl()).body().getElementsByClass(CATEGORY_DELIMITER);
            } catch (IOException e) {
                LOG.error(e);
                listener.onGrabError();
                return;
            }


            //Перебор всех групп
            groupElements.forEach(singleGroupElement -> {
                Group resultGroup = getGroupContent(singleGroupElement.child(1).select("a").get(0), null, cat);
                if (resultGroup != null) cat.addGroup(resultGroup);
            });
        }
    }

    private Group getGroupContent(Element element, Group parentGroup, Category category) {
        String name = element.text();
        if (parentGroup != null) {
            LOG.info("\t" + name);
        } else {
            LOG.info(name);
        }

        String url = element.attr("href");

        if (parentGroup != null && url.equals(parentGroup.getGroupURL())) {
            LOG.debug("Recursive group (parent and child URL's are equals)! " + url);
            return null;
        }

        Group resultGroup = new Group(name, url, parentGroup);

        Elements innerGroups = getInnerGroups(url);
        if (innerGroups != null && innerGroups.size() > 0) {
            for (Element elementG : innerGroups) {
                Group innerGroup = getGroupContent(elementG.child(1).select("a").get(0), resultGroup, category);
                if (innerGroup == null) continue;
                resultGroup.addChild(innerGroup);
            }
        }

        Elements innerLiquids = getInnerLiquids(url);
        if (innerLiquids != null && innerLiquids.size() > 0) {
            for (Element element1Liq : innerLiquids) {
                Product resultLiq = null;
                try {
                    resultLiq = parseProduct(element1Liq.attr("href"));
                } catch (IOException e) {
                    listener.onGrabberException(e.getMessage());
                }
                if (resultLiq == null) continue;
                resultLiq.setGroup(resultGroup);
                resultLiq.setCategoryID(category.getCategoryID());
                resultGroup.addLiquid(resultLiq);
                LOG.info("\t\t" + resultLiq.getName());

            }
        }
        return resultGroup.isGroupEmpty() ? null : resultGroup;
    }

    private Elements getWarehousesList() {
        try {
            Document page = Jsoup.connect("https://ilfumoshop.ru/magaziny").get();
            Elements storesListPart = page.body().getElementsByClass("col-sm-12");
            return storesListPart.select("a");

        } catch (IOException e) {
            listener.onGrabberException(e.getMessage());
            LOG.error("Error to get the stores page!");
            return null;
        }
    }

    private void insertAllCategories(List<Category> categories) {
        categories.forEach(category -> {
            listener.insertCategory(category.getName());
        });
    }

    private boolean insertAllWarehouses(Elements elements) {
        if (elements == null) {
            return false;
        }
        int region = 0;
        String city;
        String address;
        String phone;

        int index = 0;
        while (index < elements.size()) {
            Element storePart = elements.get(index);
            String text = storePart.text();
            int indexOfChar = text.indexOf(',');

            if (indexOfChar != -1) {
                city = text.substring(0, indexOfChar);
                address = text.substring(indexOfChar + 2);
                String phonePart = elements.get(index).attr("href");

                while (!phonePart.contains("tel")) {
                    index++;
                    phonePart = elements.get(index).attr("href");
                }
                indexOfChar = phonePart.indexOf('+');

                if (indexOfChar != -1) {
                    phone = phonePart.substring(indexOfChar);
                } else {
                    phone = phonePart;
                }

                switch (city) {
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
                listener.insertStore(region, city, address, phone);
                totalInsertCount++;
            }
            index++;
        }
        return true;
    }

    private void insertAllLiquids(ArrayList<Category> categories) {

        long currentTime = System.currentTimeMillis();
        categories.forEach(category -> {
            if (category.getGroups() != null) category.getGroups().forEach(this::innerGroups);
        });
        LOG.info("Finished with " + (System.currentTimeMillis() - currentTime) + "ms");
    }

    private void innerGroups(Group group) {
        if (group.isGroupHaveLiquids()) group.getProducts().forEach(product -> {
            insertProductToDB(product);
            totalInsertCount++;
        });
        if (group.isGroupHaveChild()) group.getChildGroups().forEach(this::innerGroups);
    }
}
