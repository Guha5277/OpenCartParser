package main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.time.LocalDate;
import java.util.Locale;

class SQLClient {
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

    synchronized static void commit() {
        try {
            connection.commit();
            LOG.info("Commited changes to DB Succesful!");
        } catch (SQLException e) {
            LOG.error("Failed to Commit changes to DB" + e.getMessage());
        }
    }

    synchronized static void insertCategory(String categoryName) {
        String query = String.format("INSERT INTO categories (name) VALUES ('%s')", categoryName);
        try {
            statement.execute(query);
        } catch (SQLException e) {
            LOG.error("Failed to Insert category to DB! CategoryName: " + categoryName + ", " + e.getMessage());
        }
    }

    synchronized static int getCategoryID(String categoryName) {
        int categoryID = 0;

        String query = String.format("SELECT id FROM categories WHERE name='%s'", categoryName);
        try {
            ResultSet set = statement.executeQuery(query);
            if (set.next()) {
                categoryID = set.getInt(1);
            }
        } catch (SQLException e) {
            LOG.error("Failed to Get product.Category ID! CategoryName: " + categoryName + ", " + e.getMessage());
        }

        return categoryID;
    }

    synchronized static void insertProduct(String name, String url, int price, int category, String groupName, int volume, double strength) {
        int index = name.indexOf('\'');
        if (index != -1) name = returnValidString(name, index);

        index = groupName.indexOf('\'');
        if (index != -1) groupName = returnValidString(groupName, index);

        String query = String.format(Locale.CANADA, "INSERT INTO liquids(name, url, price, category, groupName, strength, volume) VALUES('%s', '%s', %d, %d, '%s', %.1f, %d)",
                name, url, price, category, groupName, strength, volume);
        try {
            statement.execute(query);
            LOG.info("Product inserted!: " + name);
        } catch (SQLException e) {
            LOG.error("Failed to Insert new product. Product:"
                    + "\n\t\t name: " + name
                    + "\n\t\t url: " + url
                    + "\n\t\t price: " + price
                    + "\n\t\t category: " + category
                    + "\n\t\t groupName: " + groupName
                    + "\n\t\t volume: " + volume
                    + "\n\t\t strength: " + strength
                    + "\n " + e.getMessage());

        }
    }

    synchronized static void insertStore(int region, String city, String address, String phone) {
        String query = String.format("INSERT INTO warehouse(region, city, address, phone) VALUES(%d, '%s', '%s', '%s')",
                region, city, address, phone);
        try {
            statement.execute(query);
        } catch (SQLException e) {
            LOG.error("Failed to Insert new Store!"
                    + "\n\t\t region: " + region
                    + "\n\t\t city: " + city
                    + "\n\t\t address: " + address
                    + "\n\t\t phone: " + phone
                    + "\n " + e.getMessage());
        }
    }

    synchronized static boolean isProductAlreadyInDB(String url) {
        String query = String.format("SELECT name from liquids WHERE url='%s'", url);
        try {
            ResultSet set = statement.executeQuery(query);
            int size = 0;
            if (set.next()) {
                size = 1;
            }
            return size > 0;
        } catch (SQLException e) {
            LOG.error("Error when trying to get product by URL: " + e.getMessage());

        }
        return true;
    }

    synchronized static ResultSet getAllProducts() {
        String query = "SELECT * from liquids";
        try {
            return statement.executeQuery(query);
        } catch (SQLException e) {
            LOG.error("Error when trying to get all Products from DB" + e.getMessage());
            return null;
        }
    }

    synchronized static ResultSet getAllWarehouses() {
        String query = "SELECT * from warehouse";
        try {
            return statement.executeQuery(query);
        } catch (SQLException e) {
            LOG.error("Error when trying to get all product.Warehouse from DB" + e.getMessage());
            return null;
        }
    }

    private static String returnValidString(String wrongString, int index) {
        String sub1 = wrongString.substring(0, index);
        String sub2 = wrongString.substring(index);
        return sub1 + '\'' + sub2;
    }

    synchronized static void updateProductName(int id, String name) {
        String query = String.format("UPDATE liquids SET name = '%s' WHERE id = %d", name, id);
        try {
            statement.execute(query);
        } catch (SQLException e) {
            LOG.error("Failed to UPDATE product name! product_id: " + id + ", " + e.getMessage());
        }
    }

    synchronized static void updateProductGroupName(int id, String name) {
        String query = String.format("UPDATE liquids SET groupName = '%s' WHERE id = %d", name, id);
        try {
            statement.execute(query);
        } catch (SQLException e) {
            LOG.error("Failed to UPDATE product groupName! product_id: " + id + ", " + e.getMessage());
        }
    }

    synchronized static void updateProductCategory(int id, int category) {
        String query = String.format("UPDATE liquids SET category = %d WHERE id = %d", category, id);
        try {
            statement.execute(query);
        } catch (SQLException e) {
            LOG.error("Failed to UPDATE product category! product_id: " + id + ", " + e.getMessage());
        }
    }

    synchronized static void updateProductPrice(int id, int price) {
        String query = String.format("UPDATE liquids SET price = %d WHERE id = %d", price, id);
        try {
            statement.execute(query);
        } catch (SQLException e) {
            LOG.error("Failed to UPDATE product PRICE! product_id: " + id + ", " + e.getMessage());
        }
    }

    synchronized static void updateProductVolume(int id, int volume) {
        String query = String.format(Locale.CANADA, "UPDATE liquids SET volume = %d WHERE id = %d", volume, id);
        try {
            statement.execute(query);
        } catch (SQLException e) {
            LOG.error("Failed to UPDATE product VOLUME! product_id: " + id + ", " + e.getMessage());
        }
    }

    synchronized static void updateProductStrength(int id, double strength) {
        String query = String.format(Locale.CANADA, "UPDATE liquids SET strength = %.1f WHERE id = %d", strength, id);
        try {
            statement.execute(query);
        } catch (SQLException e) {
            LOG.error("Failed to UPDATE product STRENGTH! product_id: " + id + ", " + e.getMessage());
        }
    }

    synchronized static void updateProductRemains(int warehouseId, int productId, int remains) {
        String updateQuery = String.format("UPDATE product_remains SET remains = %d WHERE warehouse_id = %d AND product_id = %d ", remains, warehouseId, productId);
        String insertQuery = String.format("INSERT INTO product_remains (warehouse_id, product_id, remains) VALUES (%d, %d, %d);", warehouseId, productId, remains);
        try {
            int count = statement.executeUpdate(updateQuery);
            if (count == 0) {
                statement.execute(insertQuery);
            }
        } catch (SQLException e) {
            LOG.error("Failed to UPDATE product REMAINS! product_id: " + productId + ", warehouse_id: " + warehouseId + ", " + e.getMessage());
        }
    }

    synchronized static String getNickname(String login, String password) {
        String query = String.format("SELECT nickname FROM users WHERE username='%s' AND password='%s'", login, password);
        try {
            ResultSet set = statement.executeQuery(query);
            if (set.isClosed()) return null;
            return set.getString(1);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    synchronized static int getUserRole(String nickname) {
        String query = String.format("SELECT role FROM users WHERE nickname='%s'", nickname);
        try {
            ResultSet set = statement.executeQuery(query);
            if (set.isClosed()) return 4;
            return set.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
            return 4;
        }
    }

    static int getProductsCount() {
        String query = "SELECT COUNT(*) FROM liquids";
        try {
            ResultSet set = statement.executeQuery(query);
            if (set.isClosed()) return 0;
            return set.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    static int getWarehousesCount() {
        String query = "SELECT COUNT(*) FROM warehouse";
        try {
            ResultSet set = statement.executeQuery(query);
            if (set.isClosed()) return 0;
            return set.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    synchronized static String getUpdaterLastRun() {
        String query = "SELECT last_run FROM info WHERE process='updater'";
        try {
            ResultSet set = statement.executeQuery(query);
            if (set.isClosed()) return null;
            return set.getString(1);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    static String getResearcherLastRun() {
        String query = "SELECT last_run FROM info WHERE process='researcher'";
        try {
            ResultSet set = statement.executeQuery(query);
            if (set.isClosed()) return null;
            return set.getString(1);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    synchronized static void updateUpdaterLastRun(int pausedAt, int totalUpdated, int totalFails) {
        LocalDate date = LocalDate.now();
        String query = String.format("UPDATE info SET last_run='%d-%d-%d', paused_at=%d, total_updated=%d, total_fails=%d WHERE process='updater'", date.getYear(), date.getMonthValue(), date.getDayOfMonth(), pausedAt, totalUpdated, totalFails);
        String insertQuery = String.format("INSERT INTO info (process, last_runs, paused_at, total_updated, total_fails) VALUES ('updater', '%d-%d-%d', %d, %d, %d)", date.getYear(), date.getMonthValue(), date.getDayOfMonth(), pausedAt, totalUpdated, totalFails);
        try {
            int count = statement.executeUpdate(query);
            if (count == 0) {
                statement.execute(insertQuery);
            }
        } catch (SQLException e) {
            LOG.error("Failed to UPDATE INFO! product_id: " + e.getMessage());
        }
    }

    synchronized  static int getLastUpdatedProductPosition() {
        String query = "select paused_at from info where process='updater'";
        try {
            ResultSet set = statement.executeQuery(query);
            if (set.isClosed()) return 0;
            return set.getInt(1);
        } catch (SQLException e) {
            LOG.error("Failed to UPDATE INFO! product_id: " + e.getMessage());
        }
        return 0;
    }

    synchronized static void updateResearcherLastRun(int totalFound) {
        LocalDate date = LocalDate.now();
        String query = String.format("UPDATE info SET last_run='%d-%d-%d', total_updated=%d WHERE process='researcher'", date.getYear(), date.getMonthValue(), date.getDayOfMonth(), totalFound);
        String insertQuery = String.format("INSERT INTO info (process, last_runs, total_updated) VALUES ('researcher', '%d-%d-%d', %d)", date.getYear(), date.getMonthValue(), date.getDayOfMonth(), totalFound);
        try {
            int count = statement.executeUpdate(query);
            if (count == 0) {
                statement.execute(insertQuery);
            }
        } catch (SQLException e) {
            LOG.error("Failed to UPDATE INFO! product_id: " + e.getMessage());
        }
    }
}
