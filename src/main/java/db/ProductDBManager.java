package db;

import models.Product;
import models.ProductCriteria;
import models.User;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDBManager {
    private Connection con;

    public void initialization(String name, boolean inMemory){
        try {
            Class.forName("org.sqlite.JDBC");
//            con = DriverManager.getConnection("jdbc:sqlite::memory:");
            String dbName = inMemory ? "jdbc:sqlite::memory:" : "jdbc:sqlite:" + name;
            con = DriverManager.getConnection(dbName);

            PreparedStatement st = con.prepareStatement("create table if not exists 'product' (" +
                    "'id' INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "'name' text NOT NULL UNIQUE, " +
                    "'description' text, " +
                    "'manufacturer' text, " +
                    "'quantity' double NOT NULL, " +
                    "'price' double NOT NULL);");
            st.executeUpdate();
            st.close();

            st = con.prepareStatement("create table if not exists 'users' (" +
                    "'id' INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "'login' text NOT NULL UNIQUE, " +
                    "'password' BLOB);");
            st.executeUpdate();
            st.close();
        } catch(ClassNotFoundException e){
            throw new RuntimeException("No JDBC driver was found", e);
        } catch (SQLException e){
            throw new RuntimeException("Cannot create table", e);
        }
    }

    public void initialization(String name) {
        initialization(name, false);
    }

    public Product insertProduct(Product product) throws IllegalArgumentException{
        try {
            PreparedStatement statement = con.prepareStatement("select * from product where name = ? ");
            statement.setString(1, product.getName());
            ResultSet existingProduct = statement.executeQuery();

            if (existingProduct.next()) {
                statement.close();
                throw new IllegalArgumentException("Product with such name already exists");
            }
            statement.close();

            validateProduct(product);

            statement = con.prepareStatement("INSERT INTO " +
                    "product(name, description, manufacturer, quantity, price) " +
                    "VALUES (?, ?, ?, ?, ?)");
            //statement.setInt(1, 1);
            statement.setString(1, product.getName());
            statement.setString(2, product.getDescription());
            statement.setString(3, product.getManufacturer());
            statement.setDouble(4, product.getQuantity());
            statement.setDouble(5, product.getPrice());

            statement.executeUpdate();
            ResultSet result = statement.getGeneratedKeys();
            product.setId(result.getInt("last_insert_rowid()"));

            statement.close();

            return product;
        } catch (SQLException e){
            throw new RuntimeException("cannot insert product");
        }
    }

    public User insertUser(User user) throws IllegalArgumentException {
        try {
            PreparedStatement statement = con.prepareStatement("select * from users where " +
                    "login = ? ");
            statement.setString(1, user.getLogin());
            ResultSet existingUser = statement.executeQuery();

            if (existingUser.next()) {
                statement.close();
                throw new IllegalArgumentException("User with such login already exists");
            }
            statement.close();

            if (user.getLogin().equals("")) {
                throw new IllegalArgumentException("Login value is not allowed");
            }

            if (user.getPassword().length == 0) {
                throw new IllegalArgumentException("Password value is not allowed");
            }

             statement = con.prepareStatement("INSERT INTO " +
                    "users(login, password) " +
                    "VALUES (?, ?)");

            //statement.setInt(1, 1);
            statement.setString(1, user.getLogin());
            statement.setBytes(2, user.getPassword());

            statement.executeUpdate();
            ResultSet result = statement.getGeneratedKeys();
            user.setId(result.getInt("last_insert_rowid()"));

            statement.close();

            return user;
        } catch (SQLException e){
            throw new RuntimeException("cannot insert user");
        }
    }

    public Product getProductById(int id) {
        String sqlQuery = "SELECT * FROM product WHERE id = ?";
        try (PreparedStatement pstm = con.prepareStatement(sqlQuery);
        ) {
            pstm.setInt(1, id);
            ResultSet res = pstm.executeQuery();
            if (!res.next()) {
                throw new IllegalArgumentException("No product with such id");
            }

            return convertToProduct(res);
        } catch(SQLException e){
            throw new RuntimeException("Cannot select product by id", e);
        }
    }

    public Product getProductByName(String name) {
        String sqlQuery = "SELECT * FROM product WHERE name = ?";
        try (PreparedStatement pstm = con.prepareStatement(sqlQuery);
        ) {
            pstm.setString(1, name);
            ResultSet res = pstm.executeQuery();

            if (!res.next()) {
                throw new IllegalArgumentException("No user exists with such name");
            }

            return convertToProduct(res);
        } catch(SQLException e){
            throw new RuntimeException("Cannot select product by name", e);
        }
    }

    public User getUserById(int id) {
        String sqlQuery = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement pstm = con.prepareStatement(sqlQuery);
        ) {
            pstm.setInt(1, id);
            ResultSet res = pstm.executeQuery();

            if (!res.next()) {
                throw new IllegalArgumentException("No product exist with such id");
            }

            return convertToUser(res);
        } catch(SQLException e){
            throw new RuntimeException("Cannot select user by id", e);
        }
    }

    public User getUserByLogin(String login) {
        String sqlQuery = "SELECT * FROM users WHERE login = ?";
        try (PreparedStatement pstm = con.prepareStatement(sqlQuery);
        ) {
            pstm.setString(1, login);
            ResultSet res = pstm.executeQuery();

            if (!res.next()) {
                throw new IllegalArgumentException("No user exists with such login");
            }

            return convertToUser(res);
        } catch(SQLException e){
            throw new IllegalArgumentException("No users with login: \"" + login + "\"", e);
        }
    }

    public List<Product> getAllProducts(){
        try (Statement st = con.createStatement();
             ResultSet res = st.executeQuery("SELECT * FROM product");
        ) {
            List<Product> products = new ArrayList<>();

            while (res.next()) {
                products.add(convertToProduct(res));
            }

            return products;
        } catch(SQLException e){
            throw new RuntimeException("Cannot select products", e);
        }
    }

    public void updateProductById(int id, Product newProduct) {
        String sqlQuery = "SELECT * FROM product WHERE id = ?";
        try (PreparedStatement pstm = con.prepareStatement(sqlQuery);
        ) {
            pstm.setInt(1, id);
            ResultSet res = pstm.executeQuery();

            if (!res.next()) {
                throw new IllegalArgumentException("No product with such id");
            }
            pstm.close();
        } catch (SQLException e) {
            throw new RuntimeException("Something went wrong");
        }
        try {
            PreparedStatement statement = con.prepareStatement("select * from product where name = ? AND id != ?");
            statement.setString(1, newProduct.getName());
            statement.setInt(2, id);
            ResultSet existingProduct = statement.executeQuery();
            if (existingProduct.next()) {
                statement.close();
                throw new IllegalArgumentException("Product with such name already exists");
            }
            statement.close();

            validateProduct(newProduct);
        } catch(SQLException e) {
            throw new RuntimeException("Could not check product name uniqueness");
        }

        String sqlQueryUpdate = "UPDATE product set name = ?, " +
                "description = ? ," +
                "manufacturer = ? , " +
                "quantity = ? , " +
                "price = ? " +
                "where id = ? ";
        try (PreparedStatement pstm = con.prepareStatement(sqlQueryUpdate)) {
            pstm.setString(1, newProduct.getName());
            pstm.setString(2, newProduct.getDescription());
            pstm.setString(3, newProduct.getManufacturer());
            pstm.setDouble(4, newProduct.getQuantity());
            pstm.setDouble(5, newProduct.getPrice());
            pstm.setInt(6, id);
            pstm.executeUpdate();
            pstm.close();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to update product by id", e);
        }
    }

    public void updateProductByName(String name, Product newProduct) {
        String sqlQuery = "SELECT * FROM product WHERE name = ?";
        try (PreparedStatement pstm = con.prepareStatement(sqlQuery);
        ) {
            pstm.setString(1, name);
            ResultSet res = pstm.executeQuery();

            pstm.close();
            if (!res.next()) {
                throw new IllegalArgumentException("No product with such id");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Something went wrong");
        }
        validateProduct(newProduct);
        sqlQuery = "UPDATE product set name = ?, " +
                "description = ? ," +
                "manufacturer = ? , " +
                "quantity = ? , " +
                "price = ? " +
                "where name = ? ";
        try (PreparedStatement pstm = con.prepareStatement(sqlQuery)) {
            pstm.setString(1, newProduct.getName());
            pstm.setString(2, newProduct.getDescription());
            pstm.setString(3, newProduct.getManufacturer());
            pstm.setDouble(4, newProduct.getQuantity());
            pstm.setDouble(5, newProduct.getPrice());
            pstm.setString(6, name);
            pstm.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to update product by id", e);
        }
    }

    public void deleteProductById(int id) throws IllegalArgumentException {

        String sqlQuery = "SELECT * FROM product WHERE id = ?";
        try (PreparedStatement pstm = con.prepareStatement(sqlQuery);
        ) {
            pstm.setInt(1, id);
            ResultSet res = pstm.executeQuery();
            if (!res.next()) {
                throw new IllegalArgumentException("No product with such id");
            }

            pstm.close();
        } catch (SQLException e) {
            throw new RuntimeException("Something went wrong");
        }

        sqlQuery = "Delete FROM product where id = ?";
        try (PreparedStatement st = con.prepareStatement(sqlQuery))
        {
            st.setInt(1, id);
            st.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Cannot delete item", e);
        }
    }

    public void deleteProductByName(String name) {

        String sqlQuery = "SELECT * FROM product WHERE name = ?";
        try (PreparedStatement pstm = con.prepareStatement(sqlQuery);
        ) {
            pstm.setString(1, name);
            ResultSet res = pstm.executeQuery();

            pstm.close();
            if (!res.next()) {
                throw new IllegalArgumentException("No product with such name");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Something went wrong");
        }

        sqlQuery = "Delete FROM product where name = ?";
        try (PreparedStatement st = con.prepareStatement(sqlQuery))
        {
            st.setString(1, name);
            st.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Cannot delete product", e);
        }
    }

    public void deleteAllProducts() {
        try (PreparedStatement st = con.prepareStatement("DELETE from product");
        ) {
            st.executeUpdate();
        } catch(SQLException e){
            throw new RuntimeException("Cannot delete all products", e);
        }
        try (PreparedStatement st = con.prepareStatement("UPDATE SQLITE_SEQUENCE SET SEQ=0 WHERE NAME='product';");
        ) {
            st.executeUpdate();
        } catch(SQLException e){
            throw new RuntimeException("Cannot reset products' id's", e);
        }
    }

    public void deleteUserById(int id) throws IllegalArgumentException {

        String sqlQuery = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement pstm = con.prepareStatement(sqlQuery);
        ) {
            pstm.setInt(1, id);
            ResultSet res = pstm.executeQuery();

            pstm.close();
            if (!res.next()) {
                throw new IllegalArgumentException("No user with such id");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Something went wrong");
        }

        sqlQuery = "Delete FROM users where id = ?";
        try (PreparedStatement st = con.prepareStatement(sqlQuery))
        {
            st.setInt(1, id);
            st.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Cannot delete user", e);
        }
    }

    public void deleteAllUsers() {
        try (PreparedStatement st = con.prepareStatement("DELETE from users");
        ) {
            st.executeUpdate();
        } catch(SQLException e){
            throw new RuntimeException("Cannot delete all users", e);
        }
        try (PreparedStatement st = con.prepareStatement("UPDATE SQLITE_SEQUENCE SET SEQ=0 WHERE NAME='users';");
        ) {
            st.executeUpdate();
        } catch(SQLException e){
            throw new RuntimeException("Cannot reset users' id's", e);
        }
    }

    public List<Product> getAllByCriteria(ProductCriteria criteria){
        List<String> criterias = new ArrayList<>();

        if (criteria.getNameContains() != null)
            criterias.add(" name like '%" + criteria.getNameContains() + "%' ");
        if (criteria.getDescriptionContains() != null)
            criterias.add(" description like '%" + criteria.getDescriptionContains() + "%' ");
        if (criteria.getManufacturer() != null)
            criterias.add(" manufacturer = '" + criteria.getManufacturer() + "' ");
        if (criteria.getManufacturerContains() != null)
            criterias.add(" manufacturer like '%" + criteria.getManufacturerContains() + "%' ");
        if (criteria.getPriceFrom() != null)
            criterias.add(" price >= " + criteria.getPriceFrom() + " ");
        if (criteria.getPriceTill() != null)
            criterias.add(" price <= " + criteria.getPriceTill() + " ");
        if (criteria.getQuantityFrom() != null)
            criterias.add(" quantity >= " + criteria.getQuantityFrom() + " ");
        if (criteria.getQuantityTill() != null)
            criterias.add(" quantity <= " + criteria.getQuantityTill() + " ");
        
        String where = criterias.isEmpty() ? "" : " where " + String.join("and", criterias);

        try (Statement st = con.createStatement();
             ResultSet res = st.executeQuery("SELECT * FROM product" + where);
        ) {
            List<Product> products = new ArrayList<>();
            while (res.next()) {
                products.add(convertToProduct(res));
            }
            return products;
        }catch(SQLException e){
            throw new RuntimeException("Cannot select all products", e);
        }
    }

    private static Product convertToProduct(ResultSet resultSet) throws SQLException {
        return new Product(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getString("manufacturer"),
                resultSet.getDouble("quantity"),
                resultSet.getDouble("price")
        );
    }

    private static User convertToUser(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getInt("id"),
                resultSet.getString("login"),
                resultSet.getBytes("password")
        );
    }

    private static void validateProduct(Product product) throws IllegalArgumentException{
        if (product.getPrice() < 0)
            throw new IllegalArgumentException("Product price is invalid");

        if (product.getQuantity() < 0)
            throw new IllegalArgumentException("Product quantity is invalid");

        if (product.getDescription() == null || product.getDescription().isEmpty())
            throw new IllegalArgumentException("Product description must not be empty");

        if (product.getManufacturer() == null || product.getManufacturer().isEmpty())
            throw new IllegalArgumentException("Product manufacturer must not be empty");

        if (product.getName() == null || product.getName().isEmpty())
            throw new IllegalArgumentException("Product name must not be empty");
    }

    public static void main(String[] args) {
        ProductDBManager dbManager = new ProductDBManager();
        dbManager.initialization("ProductDB");
        dbManager.deleteAllProducts();
        dbManager.deleteAllUsers();
        dbManager.insertProduct(new Product("Watch", "this shows Swiss time",
                "Merida",2, 10000));
        dbManager.insertProduct(new Product("Grechka", "this is tasty",
                "Nike", 10, 1.2));
        dbManager.insertProduct(new Product("Skoda", "this is a car",
                "Check Republic", 7, 20000));
        dbManager.insertProduct(new Product("Button", "this is not grechka",
                "Apple", 100, 5));
        dbManager.insertProduct(new Product(5, "Elephant", "this should not be here", "ZooEee", 1, 15000));
        dbManager.deleteProductById(3);

        int id = dbManager.insertUser(new User("l", "p".getBytes(StandardCharsets.UTF_8))).getId();

        System.out.println(dbManager.getUserById(id));
        System.out.println(dbManager.getUserByLogin("l"));
    }
}
