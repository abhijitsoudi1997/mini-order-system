package com.abhijit.order_service.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;

class InventoryClientTest {

    @Test
    void clientIsLookedUpByServiceNameNotByFixedUrl() {
        FeignClient annotation = InventoryClient.class.getAnnotation(FeignClient.class);

        assertEquals("inventory-service", annotation.name());
        assertTrue(annotation.url().isEmpty());
    }
}