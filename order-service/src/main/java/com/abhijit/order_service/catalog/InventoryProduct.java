package com.abhijit.order_service.catalog;

public record InventoryProduct(Long id, String sku, String name, String category,
        java.math.BigDecimal price, int quantity) {

}
