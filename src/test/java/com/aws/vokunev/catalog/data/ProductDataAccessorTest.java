package com.aws.vokunev.catalog.data;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Tests for ProductCatalogAccessor")
public class ProductDataAccessorTest {

    /**
     * Initializing variables before we run the tests. Use @BeforeAll for
     * initializing static variables at the start of the test class execution.
     * Use @BeforeEach for initializing variables before each test is run.
     */
    @BeforeAll
    static void setup() {
    }

    /**
     * De-initializing variables after we run the tests. Use @AfterAll for
     * de-initializing static variables at the end of the test class execution.
     * Use @AfterEach for de-initializing variables at the end of each test.
     */
    @AfterAll
    static void tearDown() {
    }

    @Test
    @DisplayName("Test for retrieving product catalog data")
    void testProductCatalog() {
        ArrayList<CatalogItem> catalog = ProductDataAccessor.getProductCatalog();
        assertNotNull(catalog);
        assertTrue(catalog.size() > 0);
    }

    @Test
    @DisplayName("Test for retrieving product object")
    void testExistingProduct() {
        Product product = ProductDataAccessor.getProduct(213);
        assertNotNull(product);
    }    

    @Test
    @DisplayName("Test for retrieving product object")
    void testNonexistingProduct() {
        try {
            Product product = ProductDataAccessor.getProduct(913);
            Assert.fail("Expected an exception for a non-exiting product");
        } catch (Exception ex) {}
    }    
}