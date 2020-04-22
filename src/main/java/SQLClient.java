import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

public class SQLClient {
    private static Connection connection;
    private static Statement statement;
    private static Logger LOG;

    static {
        LOG = LogManager.getLogger();
    }

    synchronized static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:liquidBase.db");
            connection.setAutoCommit(false);
            statement = connection.createStatement();
            LOG.info("Connected to DB");
        } catch (ClassNotFoundException | SQLException e) {
            LOG.error("Failed to Connect to DB! " + e.getMessage());
        }
    }

    synchronized static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            LOG.error("Failed to Close the connection" + e.getMessage());
        }
        LOG.info("Disonnected from DB");
    }

    synchronized static void commit(){
        try {
            connection.commit();
            LOG.info("Commited changes to DB Succesful!");
        } catch (SQLException e) {
            LOG.error("Failed to Commit changes to DB" + e.getMessage());
        }
    }

    synchronized static void insertCategory(Category category) {
        String query = String.format("INSERT INTO categories (name) VALUES ('%s')", category.getName());
        try {
            statement.execute(query);
        } catch (SQLException e) {
            LOG.error("Failed to Insert category to DB! CategoryName: " + category.getName() + ", " + e.getMessage());
        }
    }

    synchronized static int getCategoryID(Category category) {
        String categoryName = category.getName();
        int categoryID = 0;

        String query = String.format("SELECT id FROM categories WHERE name='%s'", categoryName);
        try {
            ResultSet set = statement.executeQuery(query);
            if (set.next()) {
                categoryID = set.getInt(1);
            }
        } catch (SQLException e) {
            LOG.error("Failed to Get Category ID! CategoryName: " + categoryName + ", " + e.getMessage());
        }

        return categoryID;
    }

    synchronized static void insertNewProduct(Product product) {
        String name = product.getName();
        String url = product.getURL();
        int price = product.getPrice();
        int category = product.getCategoryID();
        String groupName = product.getGroup().getGroupName();

        int index = name.indexOf('\'');
        if(index != -1) name = returnValidString(name, index);

        index = groupName.indexOf('\'');
        if(index != -1) groupName = returnValidString(groupName, index);

        String query = String.format("INSERT INTO liquids(name, url, price, category, groupName) VALUES('%s', '%s', %d, %d, '%s')",
                name, url, price, category, groupName);
        try {
            statement.execute(query);
        } catch (SQLException e) {
            LOG.error("Failed to Insert new Product!"
                        +"\n\t\t name: " + name
                        +"\n\t\t url: " + url
                        +"\n\t\t price: " + price
                        +"\n\t\t category: " + category
                        +"\n\t\t groupName: " + groupName
            + "\n " + e.getMessage());
        }
    }

    synchronized static void insertNewStore(int region, String city, String address, String phone){
        String query = String.format("INSERT INTO shop_list(region, city, address, phone) VALUES(%d, '%s', '%s', '%s')",
                region, city, address, phone);
        try {
            statement.execute(query);
        } catch (SQLException e) {
            LOG.error("Failed to Insert new Store!"
                    +"\n\t\t region: " + region
                    +"\n\t\t city: " + city
                    +"\n\t\t address: " + address
                    +"\n\t\t phone: " + phone
                    + "\n " + e.getMessage());
        }
    }

    private static String returnValidString(String wrongString, int index){
        String sub1 = wrongString.substring(0, index);
        String sub2 = wrongString.substring(index);
        return sub1 + '\'' + sub2;
    }

}
