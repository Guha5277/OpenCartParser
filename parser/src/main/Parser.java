package main;

import main.product.Category;
import main.product.Group;
import main.product.Product;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class Parser {
    static final Logger LOG = LogManager.getLogger("ParserLogger");

    private final String CATEGORY_DELIMITER = "col-lg-4 col-md-4 col-sm-6 col-xs-12";
    private static final String INNER_LIQUID_DELIMITER = "product-title";

    Document downloadPage(String url) throws IOException {
        //LOG.info("Downloading page: " + url);
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
            //LOG.info("Getting inner groups...");
            return Jsoup.connect(url).get().body().getElementsByClass(CATEGORY_DELIMITER);
        } catch (IOException e) {
            LOG.error("Failed to get inner group " + e.getMessage());
            return null;
        }
    }

    /*TODO rename method to getInnerProducts*/
    Elements getInnerLiquids(String url) {
        try {
            //LOG.info("Getting inner liquids...");
            return Jsoup.connect(url).get().body().getElementsByClass(INNER_LIQUID_DELIMITER);
        } catch (IOException e) {
            LOG.error("Failed to get inner liquids " + e.getMessage());
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

    Product parseProduct(String url) throws IOException {
        LOG.info("Parsing product: " + url);
        Product result;
        String name;
        String groupName;
        String categoryName;
        int price;
        int categoryID;

        Document document = Jsoup.connect(url).get();
        Elements elements = document.body().getElementsByClass("breadcrumb").select("li");
        if (elements.size() == 0) {
            LOG.error("Failed to parse product page: " + url);
            //System.out.println("Wrong product page!: " + url);
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

        result = new Product(name, url, price, new Group(groupName, null), categoryID);

        parseVolume(result);
        parseStrength(result);

        if (result.getVolume() > 0 && result.getStrength() < 0) {
            result.setStrength(3.0);
        }
        //LOG.info("Product parsed: " + url + ", " + name);
        return result;
    }

    private int parseStringPriceToInt(String price) {
        int cutIndex = price.indexOf("р");
        return Integer.parseInt(price.substring(0, cutIndex));
    }

    List<Element> getCategoryElements(Category category) throws IOException {
        List<Element> groupList = new ArrayList<>();
        Elements groupElements = downloadPage(category.getUrl()).body().getElementsByClass(CATEGORY_DELIMITER);
        groupElements.forEach(singleGroupElement -> {
            groupList.add(singleGroupElement.child(1).select("a").get(0));
        });

        return groupList;
    }

    private void parseVolume(Product product) {
        String[] nameParts = product.getName().split(" ");
        int volumeIndex;

        volumeIndex = findVolumeIndex(nameParts);

        if (volumeIndex < 0) {
            product.setVolume(0);
        } else {
            String volumePart = nameParts[volumeIndex];
            product.setVolume(stringToVolume(volumePart));

            StringBuilder newName = new StringBuilder();

            for (int i = 0; i < nameParts.length; i++) {
                if (i == volumeIndex) {
                    if (i == nameParts.length - 1) continue;
                    if (nameParts[i + 1].contains("мл") || nameParts[i + 1].contains("ml")) i++;
                    continue;
                }
                newName.append(nameParts[i]);
                if (i != nameParts.length - 1) newName.append(" ");
            }
            product.setName(newName.toString());
        }
    }

    private int findVolumeIndex(String[] nameParts) {
        int index = -1;

        for (int i = 0; i < nameParts.length; i++) {
            if (nameParts[i].contains("мл")) {
                if (nameParts[i].contains("мг/мл")) {
                    if (i >= nameParts.length - 1) break;
                    i++;
                    while (i < nameParts.length) {
                        if (nameParts[i].contains("мл")) break;
                        i++;
                    }
                    if (i >= nameParts.length) break;
                }
                if (nameParts[i].length() == 2) {
                    index = i - 1;
                } else if (nameParts[i].length() < 6 || nameParts[i].indexOf('*') > -1) {
                    index = i;
                }
                break;
            }
        }
        if (index < 0) {
            for (int i = 0; i < nameParts.length; i++) {
                if (nameParts[i].contains("ml")) {
                    if (nameParts[i].length() == 2) {
                        index = i - 1;
                    } else {
                        index = i;
                    }
                }
            }
        }
        if (index < 0) {
            for (int i = 0; i < nameParts.length; i++) {
                if (nameParts[i].contains("мг")) {
                    if (nameParts[i].length() == 2) {
                        index = i - 1;
                        break;
                    }
                }
            }
        }
        return index;
    }

    private int stringToVolume(String volume) {
        int length = volume.length();

        if ((volume.contains("мл") || volume.contains("ml")) && length < 7) {
            volume = volume.substring(0, length - 2);
        }

        int multIndex = volume.indexOf('*');
        int pointIndex = volume.indexOf('.');

        if (multIndex > -1) {
            String count = volume.substring(0, multIndex);
            String vol = volume.substring(multIndex + 1);
            return Integer.parseInt(count) * Integer.parseInt(vol);
        } else if (pointIndex > -1) {
            String vol = volume.substring(0, pointIndex);
            return Integer.parseInt(vol);
        } else {
            try {
                return Integer.parseInt(volume);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    private void parseStrength(Product product) {
        String[] nameParts = product.getName().split(" ");
        int strengthIndex = -1;

        strengthIndex = findStrengthIndex(nameParts);

        if (strengthIndex == -1) {
            product.setStrength(-1.0d);
        } else {
            product.setStrength(stringToStrength(nameParts[strengthIndex]));
            StringBuilder newName = new StringBuilder();

            for (int i = 0; i < nameParts.length; i++) {
                if (i == strengthIndex) {
                    if (i == nameParts.length - 1) continue;
                    String part = nameParts[i + 1];
                    if (part.contains("мг/мл") || part.contains("мл/мг") || part.contains("мг") || part.contains("%"))
                        i++;
                    //if (part.contains("мг/мл") || part.contains("мл/мг") ||  part.contains("мг") ||  part.contains("%")) i++;
                    continue;
                }
                newName.append(nameParts[i]);
                if (i != nameParts.length - 1) newName.append(" ");
            }
            product.setName(newName.toString());
        }
    }

    private int findStrengthIndex(String[] nameParts) {
        int index = -1;

        for (int i = 0; i < nameParts.length; i++) {
            String part = nameParts[i];
            if (part.contains("мг/мл") || part.contains("мл/мг") || part.contains("мг/м")) {
                if (nameParts[i].length() > 5) {
                    index = i;
                    break;
                }
                index = i - 1;
                break;
            }
        }

        if (index == -1) {
            for (int i = 0; i < nameParts.length; i++) {
                String part = nameParts[i];
                if (part.contains("мг/")) {
                    if (nameParts[i].length() > 3) {
                        index = i;
                        break;
                    }
                    index = i - 1;
                    break;
                }

                if (part.contains("мг")) {
                    if (nameParts[i].length() > 2) {
                        index = i;
                        break;
                    }
                    index = i - 1;
                    break;
                }
            }
        }

        if (index == -1) {
            for (int i = 0; i < nameParts.length; i++) {
                String part = nameParts[i];
                if (part.contains("%")) {
                    if (nameParts[i].length() > 1) {
                        index = i;
                        break;
                    }
                    index = i - 1;
                    break;
                }
            }
        }

        return index;
    }

    private double stringToStrength(String strength) {
        int length = strength.length();
        if (strength.contains("мг/мл") || strength.contains("мл/мг")) {
            strength = strength.substring(0, length - 5);
        }

        if (strength.contains("мг/")) {
            strength = strength.substring(0, length - 3);
        }

        if (strength.contains("мг")) {
            strength = strength.substring(0, length - 2);
        }

        if (strength.contains("/")) {
            strength = strength.split("/")[0];
        }

        if (strength.contains("%")) {
            strength = strength.split("%")[0];
        }


        int commaIndex = strength.indexOf(',');
        if (commaIndex > 0) {
            strength = strength.replace(',', '.');
        }

        try {
            return Double.parseDouble(strength);
        } catch (NumberFormatException e) {
            return -1.0d;
        }
    }

    void insertProductToDB(Product product) {
        SQLClient.insertProduct(product.getName(), product.getURL(), product.getPrice(), product.getCategoryID(), product.getGroup().getName(), product.getVolume(), product.getStrength());
    }
}