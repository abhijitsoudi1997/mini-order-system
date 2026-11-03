package com.abhijit.inventory_service.product;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.transaction.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ProductReservationTest {
	
	@Autowired
    private ProductRepository repository;

    private Product saveProduct(String sku, int quantity) {
        Product p = new Product();
        p.setSku(sku);
        p.setName("Test " + sku);
        p.setCategory("Test");
        p.setPrice(new BigDecimal("10.00"));
        p.setQuantity(quantity);
        return repository.save(p);
    }

    @Test
    void reserveIncreasesReservedQuantity() {
        saveProduct("SKU-RES-1", 5);

        int rows = repository.reserve("SKU-RES-1", 3);

        assertEquals(1, rows);
        assertEquals(3, repository.findBySkuAndActiveTrue("SKU-RES-1").orElseThrow().getReservedQuantity());
    }

    @Test
    void secondReserveThatWouldOversellIsRefused() {
        saveProduct("SKU-RES-2", 5);

        assertEquals(1, repository.reserve("SKU-RES-2", 3));   // 3 of 5 reserved
        assertEquals(0, repository.reserve("SKU-RES-2", 3));   // would be 6 of 5, refused

        assertEquals(3, repository.findBySkuAndActiveTrue("SKU-RES-2").orElseThrow().getReservedQuantity());
    }

    @Test
    void reserveExactlyTheAvailableStockWorks() {
        saveProduct("SKU-RES-3", 5);

        assertEquals(1, repository.reserve("SKU-RES-3", 5));
        assertEquals(0, repository.reserve("SKU-RES-3", 1));
    }

    @Test
    void releaseGivesStockBackAndCannotGoBelowZero() {
        saveProduct("SKU-RES-4", 5);
        repository.reserve("SKU-RES-4", 2);

        assertEquals(1, repository.release("SKU-RES-4", 2));
        assertEquals(0, repository.release("SKU-RES-4", 1));   // nothing reserved any more

        assertEquals(0, repository.findBySkuAndActiveTrue("SKU-RES-4").orElseThrow().getReservedQuantity());
    }

}
