package com.abhijit.order_service.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.abhijit.order_service.order.InventoryUnavailableException;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;

@ExtendWith(MockitoExtension.class)
public class InventoryProductCatalogTest {
	
	@Mock
    private InventoryClient client;               // fake Feign client: no network, no Inventory needed

    @InjectMocks
    private InventoryProductCatalog catalog;      // real class, with the fake client injected

    // Feign exceptions need a Request object, so build a dummy one
    private Request dummyRequest() {
        return Request.create(Request.HttpMethod.GET, "/products/sku/X",
                Map.of(), null, StandardCharsets.UTF_8, new RequestTemplate());
    }

    @Test
    void mapsInventoryProductToProductInfo() {
        when(client.getBySku("SKU-1001")).thenReturn(
            new InventoryProduct(1L, "SKU-1001", "Laptop", "Electronics", new BigDecimal("55000.00"), 10));

        Optional<ProductInfo> result = catalog.findBySku("SKU-1001");

        ProductInfo info = result.orElseThrow();
        assertEquals("SKU-1001", info.sku());
        assertEquals("Laptop", info.name());
        assertEquals(new BigDecimal("55000.00"), info.unitPrice());
        assertEquals(10, info.availableQuantity());
    }

    @Test
    void returnsEmptyWhenInventoryReturns404() {
        when(client.getBySku("SKU-9999"))
            .thenThrow(new FeignException.NotFound("not found", dummyRequest(), null, null));

        assertTrue(catalog.findBySku("SKU-9999").isEmpty());
    }

    @Test
    void throwsUnavailableWhenInventoryReturns500() {
        when(client.getBySku("SKU-1001"))
            .thenThrow(new FeignException.InternalServerError("boom", dummyRequest(), null, null));

        assertThrows(InventoryUnavailableException.class, () -> catalog.findBySku("SKU-1001"));
    }


}
