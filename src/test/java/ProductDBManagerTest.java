import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ProductDBManagerTest {

    private static final ProductDBManager PRODUCT_DB_MANAGER = new ProductDBManager();

    @BeforeAll
    static void initDB() {
        PRODUCT_DB_MANAGER.initialization("ProductDBForTests", true);
    }

    @BeforeEach
    void addDataToDB() {
        PRODUCT_DB_MANAGER.insertProduct(new Product("Watch", "this shows Swiss time",
                "Merida",2, 10000));
        PRODUCT_DB_MANAGER.insertProduct(new Product("Grechka", "this is tasty",
                "Nike", 10, 1.2));
        PRODUCT_DB_MANAGER.insertProduct(new Product("Skoda", "this is a car",
                "Czech Republic", 7, 20000));
        PRODUCT_DB_MANAGER.insertProduct(new Product("Button", "this is not grechka",
                "Apple", 100, 5));
        PRODUCT_DB_MANAGER.insertProduct(new Product(5, "Elephant", "this should not be here",
                        "ZooEee", 1, 15000));
    }

    @AfterEach
    void clearDB() {
        PRODUCT_DB_MANAGER.deleteAll();
    }

    @Test
    void shouldInsertProduct() {
        org.assertj.core.api.Assertions.assertThat(PRODUCT_DB_MANAGER.getAll())
                .extracting(Product::getName, Product::getPrice)
                .containsExactly(
                        tuple("Watch", 10000.0),
                        tuple("Grechka", 1.2),
                        tuple("Skoda", 20000.0),
                        tuple("Button", 5.0),
                        tuple("Elephant", 15000.0)
                );
    }

    @Test
    void shouldGetProductById() {
        Assertions.assertEquals(new Product(1, "Watch", "this shows Swiss time",
                "Merida",2, 10000), PRODUCT_DB_MANAGER.getById(1));
    }

    @Test
    void shouldGetProductByName() {
        Assertions.assertEquals(new Product(2, "Grechka", "this is tasty",
                "Nike", 10, 1.2), PRODUCT_DB_MANAGER.getByName("Grechka"));
    }

    @Test
    void shouldUpdateById() {
        PRODUCT_DB_MANAGER.updateById(2, new Product("Grech", "this is grech",
                "Adidas", 1, 0.5));
        Assertions.assertEquals(new Product(2, "Grech", "this is grech",
                "Adidas", 1, 0.5), PRODUCT_DB_MANAGER.getById(2));
    }

    @Test
    void shouldUpdateByName() {
        PRODUCT_DB_MANAGER.updateByName("Grechka", new Product("Grech", "this is grech",
                "Adidas", 1, 0.5));
        Assertions.assertEquals(new Product(2, "Grech", "this is grech",
                "Adidas", 1, 0.5), PRODUCT_DB_MANAGER.getByName("Grech"));
    }

    @Test
    void shouldDeleteProductById() {
        PRODUCT_DB_MANAGER.deleteById(1);
        org.assertj.core.api.Assertions.assertThat(PRODUCT_DB_MANAGER.getAll())
                .extracting(Product::getId)
                .doesNotContain(1);
    }

    @Test
    void shouldDeleteProductByName() {
        PRODUCT_DB_MANAGER.deleteByName("Skoda");
        org.assertj.core.api.Assertions.assertThat(PRODUCT_DB_MANAGER.getAll())
                .extracting(Product::getName)
                .doesNotContain("Skoda");
    }

    @Test
    void shouldDeleteAll() {
        PRODUCT_DB_MANAGER.deleteAll();
        Assertions.assertEquals(PRODUCT_DB_MANAGER.getAll().size(), 0);
    }

    private static Stream<Arguments> filtersArgumentProvider() {
        return  Stream.of(
                Arguments.of(
                        new ProductCriteria(),
                        List.of(
                                new Product(1, "Watch", "this shows Swiss time", "Merida", 2, 10000),
                                new Product(2, "Grechka", "this is tasty", "Nike", 10, 1.2),
                                new Product(3, "Skoda", "this is a car", "Czech Republic", 7, 20000),
                                new Product(4, "Button", "this is not grechka", "Apple", 100, 5),
                                new Product(5, "Elephant", "this should not be here", "ZooEee", 1, 15000))
                ),
                Arguments.of(
                        new ProductCriteria().setNameContains("a"),
                        List.of(
                                new Product(1, "Watch", "this shows Swiss time", "Merida", 2, 10000),
                                new Product(2, "Grechka", "this is tasty", "Nike", 10, 1.2),
                                new Product(3, "Skoda", "this is a car", "Czech Republic", 7, 20000),
                                new Product(5, "Elephant", "this should not be here", "ZooEee", 1, 15000)
                        )
                ),
                Arguments.of(
                        new ProductCriteria().setNameContains("nothing contains"),
                        List.of()
                ),
                Arguments.of(
                        new ProductCriteria().setDescriptionContains(" is "),
                        List.of(
                                new Product(2, "Grechka", "this is tasty", "Nike", 10, 1.2),
                                new Product(3, "Skoda", "this is a car", "Czech Republic", 7, 20000),
                                new Product(4, "Button", "this is not grechka", "Apple", 100, 5)
                        )
                ),
                Arguments.of(
                        new ProductCriteria().setManufacturer("Czech Republic"),
                        List.of(
                                new Product(3, "Skoda", "this is a car", "Czech Republic", 7, 20000)
                        )
                ),
                Arguments.of(
                        new ProductCriteria().setManufacturer("No such manufacturer"),
                        List.of()
                ),
                Arguments.of(
                        new ProductCriteria().setManufacturerContains("i"),
                        List.of(
                                new Product(1, "Watch", "this shows Swiss time", "Merida", 2, 10000),
                                new Product(2, "Grechka", "this is tasty", "Nike", 10, 1.2),
                                new Product(3, "Skoda", "this is a car", "Czech Republic", 7, 20000)
                        )
                ),
                Arguments.of(
                        new ProductCriteria().setPriceFrom(10000.0),
                        List.of(
                                new Product(1, "Watch", "this shows Swiss time", "Merida", 2, 10000),
                                new Product(3, "Skoda", "this is a car", "Czech Republic", 7, 20000),
                                new Product(5, "Elephant", "this should not be here", "ZooEee", 1, 15000)
                        )
                ),
                Arguments.of(
                        new ProductCriteria().setPriceTill(100.0),
                        List.of(
                                new Product(2, "Grechka", "this is tasty", "Nike", 10, 1.2),
                                new Product(4, "Button", "this is not grechka", "Apple", 100, 5)
                        )
                ),
                Arguments.of(
                        new ProductCriteria()
                                .setPriceFrom(3.0)
                                .setPriceTill(15000.0),
                        List.of(
                                new Product(1, "Watch", "this shows Swiss time", "Merida", 2, 10000),
                                new Product(4, "Button", "this is not grechka", "Apple", 100, 5),
                                new Product(5, "Elephant", "this should not be here", "ZooEee", 1, 15000)
                        )
                ),
                Arguments.of(
                        new ProductCriteria().setQuantityFrom(100.0),
                        List.of(
                                new Product(4, "Button", "this is not grechka", "Apple", 100, 5)
                        )
                ),
                Arguments.of(
                        new ProductCriteria().setQuantityTill(99.0),
                        List.of(
                                new Product(1, "Watch", "this shows Swiss time", "Merida", 2, 10000),
                                new Product(2, "Grechka", "this is tasty", "Nike", 10, 1.2),
                                new Product(3, "Skoda", "this is a car", "Czech Republic", 7, 20000),
                                new Product(5, "Elephant", "this should not be here", "ZooEee", 1, 15000)
                        )
                ),
                Arguments.of(
                        new ProductCriteria()
                                .setQuantityFrom(3.0)
                                .setQuantityTill(50.0),
                        List.of(
                                new Product(2, "Grechka", "this is tasty", "Nike", 10, 1.2),
                                new Product(3, "Skoda", "this is a car", "Czech Republic", 7, 20000)
                        )
                ),
                Arguments.of(
                        new ProductCriteria()
                                .setPriceFrom(5000.0)
                                .setManufacturerContains("e")
                                .setQuantityFrom(2.0)
                                .setDescriptionContains("is"),
                        List.of(
                                new Product(1, "Watch", "this shows Swiss time", "Merida", 2, 10000),
                                new Product(3, "Skoda", "this is a car", "Czech Republic", 7, 20000)
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource("filtersArgumentProvider")
    void shouldSelectByFiltersParametrized(ProductCriteria pc, List<Product> products) {
        List<Product> res = PRODUCT_DB_MANAGER.getAllByCriteria(pc);
        assertThat(res)
                .containsExactlyElementsOf(products);
    }
}
