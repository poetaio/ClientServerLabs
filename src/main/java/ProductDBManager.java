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
        } catch(ClassNotFoundException e){
            throw new RuntimeException("No JDBC driver was found", e);
        } catch (SQLException e){
            throw new RuntimeException("Cannot create table", e);
        }
    }

    public void initialization(String name) {
        initialization(name, false);
    }

    public Product insertProduct(Product product){
        try {
            PreparedStatement statement = con.prepareStatement("INSERT INTO " +
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

    public Product getById(int id) {
        String sqlQuery = "SELECT * FROM product WHERE id = ?";
        try (PreparedStatement pstm = con.prepareStatement(sqlQuery);
        ) {
            pstm.setInt(1, id);
            ResultSet res = pstm.executeQuery();

            return convertToProduct(res);
        } catch(SQLException e){
            throw new RuntimeException("Cannot select product by id", e);
        }
    }

    public Product getByName(String name) {
        String sqlQuery = "SELECT * FROM product WHERE name = ?";
        try (PreparedStatement pstm = con.prepareStatement(sqlQuery);
        ) {
            pstm.setString(1, name);
            ResultSet res = pstm.executeQuery();

            return convertToProduct(res);
        } catch(SQLException e){
            throw new RuntimeException("Cannot select product by name", e);
        }
    }

    public List<Product> getAll(){
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

    public void updateById(int id, Product newProduct) {
        String sqlQuery = "UPDATE product set name = ?, " +
                "description = ? ," +
                "manufacturer = ? , " +
                "quantity = ? , " +
                "price = ? " +
                "where id = ? ";
        try (PreparedStatement pstm = con.prepareStatement(sqlQuery)) {
            pstm.setString(1, newProduct.getName());
            pstm.setString(2, newProduct.getDescription());
            pstm.setString(3, newProduct.getManufacturer());
            pstm.setDouble(4, newProduct.getQuantity());
            pstm.setDouble(5, newProduct.getPrice());
            pstm.setInt(6, id);
            pstm.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to update product by id", e);
        }
    }

    public void updateByName(String name, Product newProduct) {
        String sqlQuery = "UPDATE product set name = ?, " +
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

    public void deleteById(int id) {
        String sqlQuery = "Delete FROM product where id = ?";
        try (PreparedStatement st = con.prepareStatement(sqlQuery))
        {
            st.setInt(1, id);
            st.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Cannot delete item", e);
        }
    }

    public void deleteByName(String name) {
        String sqlQuery = "Delete FROM product where name = ?";
        try (PreparedStatement st = con.prepareStatement(sqlQuery))
        {
            st.setString(1, name);
            st.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Cannot delete item", e);
        }
    }

    public void deleteAll() {
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

    public static void main(String[] args) {
        ProductDBManager dbManager = new ProductDBManager();
        dbManager.initialization("ProductDB");
        dbManager.deleteAll();
        dbManager.insertProduct(new Product("Watch", "this shows Swiss time",
                "Merida",2, 10000));
        dbManager.insertProduct(new Product("Grechka", "this is tasty",
                "Nike", 10, 1.2));
        dbManager.insertProduct(new Product("Skoda", "this is a car",
                "Check Republic", 7, 20000));
        dbManager.insertProduct(new Product("Button", "this is not grechka",
                "Apple", 100, 5));
        dbManager.insertProduct(new Product(5, "Elephant", "this should not be here", "ZooEee", 1, 15000));
        dbManager.deleteById(3);
    }
}
