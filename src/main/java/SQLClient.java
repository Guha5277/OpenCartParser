import java.sql.*;

public class SQLClient {
    private static Connection connection;
    private static Statement statement;

    synchronized static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:liquidBase.db");
            statement = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    synchronized static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    synchronized static void insertCategory(Category category) {
        String query = String.format("INSERT INTO categories (name) VALUES ('%s')", category.getName());
        try {
            statement.execute(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    synchronized static int getCategoryID(Category category){
        String categoryName = category.getName();
        int categoryID = 0;

        String query = String.format("SELECT id FROM categories WHERE name='%s'", categoryName);
        try {
            ResultSet set = statement.executeQuery(query);
            if(set.next()){
                categoryID = set.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return categoryID;
    }

    synchronized static void insertNewLiquid(Liquid liquid) {
        String query = String.format("INSERT INTO liquids (name, url, price, group) " +
                "VALUES (%s, %s, %d, %s, %s)", liquid.getName(), liquid.getURL(), liquid.getPrice(), liquid.getGroup());
    }


}
