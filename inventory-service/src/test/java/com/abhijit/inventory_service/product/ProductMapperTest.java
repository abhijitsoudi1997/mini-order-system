package com.abhijit.inventory_service.product;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.abhijit.inventory_service.product.Product;
import com.abhijit.inventory_service.product.ProductMapper;
import com.abhijit.inventory_service.product.ProductRequest;
import com.abhijit.inventory_service.product.ProductResponse;

public class ProductMapperTest {
	
	private final ProductMapper mapper = new ProductMapper();

    @Test
    void toEntity_copiesAllFields() {
        ProductRequest request = new ProductRequest("SKU-9", "Pen", "Stationery", new BigDecimal("20.00"), 100);

        Product product = mapper.toEntity(request);

        assertEquals("SKU-9", product.getSku());
        assertEquals("Pen", product.getName());
        assertEquals("Stationery", product.getCategory());
        assertEquals(new BigDecimal("20.00"), product.getPrice());
        assertEquals(100, product.getQuantity());
    }

    @Test
    void toResponse_copiesAllFields() {
        Product product = new Product("SKU-9", "Pen", "Stationery", new BigDecimal("20.00"), 100);

        ProductResponse response = mapper.toResponse(product);

        assertEquals("SKU-9", response.sku());
        assertEquals("Pen", response.name());
        assertEquals(100, response.quantity());
    }

}
