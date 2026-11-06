package com.abhijit.order_service.catalog;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.abhijit.order_service.order.InsufficientStockException;
import com.abhijit.order_service.order.InventoryUnavailableException;
import com.abhijit.order_service.order.StockReleaseRejectedException;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

@SpringBootTest
@ActiveProfiles("resilience")
class ResilienceIntegrationTest {

    @MockitoBean
    private InventoryClient client;                  // replaces the real Feign client

    @Autowired
    private ProductCatalog catalog;                  // the real InventoryProductCatalog, with retry + breaker

    @Autowired
    private StockReservation reservation;            // the real InventoryStockReservation

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private final List<StockLine> lines = List.of(new StockLine("SKU-1001", 1));

    @BeforeEach
    void resetBreaker() {
        circuitBreakerRegistry.circuitBreaker("inventory").reset();   // state is shared between tests
    }

    private Request dummyRequest() {
        return Request.create(Request.HttpMethod.GET, "/x", Map.of(), null,
                StandardCharsets.UTF_8, new RequestTemplate());
    }

    private FeignException serverError() {
        return new FeignException.InternalServerError("boom", dummyRequest(), null, null);
    }

    private InventoryProduct laptop() {
        return new InventoryProduct(1L, "SKU-1001", "Laptop", "Electronics", new BigDecimal("55000.00"), 10);
    }

    @Test
    void lookupSucceedsAfterTwoFailures() {
        when(client.getBySku("SKU-1001"))
                .thenThrow(serverError()).thenThrow(serverError()).thenReturn(laptop());

        Optional<ProductInfo> result = catalog.findBySku("SKU-1001");

        assertTrue(result.isPresent());
        verify(client, times(3)).getBySku("SKU-1001");
    }

    @Test
    void lookupGivesUpAfterThreeAttempts() {
        when(client.getBySku("SKU-1001")).thenThrow(serverError());

        assertThrows(InventoryUnavailableException.class, () -> catalog.findBySku("SKU-1001"));
        verify(client, times(3)).getBySku("SKU-1001");
    }

    @Test
    void notFoundIsNotRetried() {
        when(client.getBySku("SKU-9999"))
                .thenThrow(new FeignException.NotFound("nf", dummyRequest(), null, null));

        assertTrue(catalog.findBySku("SKU-9999").isEmpty());
        verify(client, times(1)).getBySku("SKU-9999");
    }

    @Test
    void reserveIsRetriedAfterATransientFailure() {
        doThrow(serverError()).doNothing().when(client).reserve(any());

        reservation.reserve("ORD-1", lines);

        verify(client, times(2)).reserve(any());
    }

    @Test
    void reserveRefusedForStockIsNotRetried() {
        doThrow(new FeignException.Conflict("conflict", dummyRequest(), null, null))
                .when(client).reserve(any());

        assertThrows(InsufficientStockException.class, () -> reservation.reserve("ORD-1", lines));
        verify(client, times(1)).reserve(any());
    }

    @Test
    void releaseOfUnknownOrderIsNotRetried() {
        doThrow(new FeignException.NotFound("nf", dummyRequest(), null, null))
                .when(client).release(any());

        assertThrows(StockReleaseRejectedException.class, () -> reservation.release("ORD-1", lines));
        verify(client, times(1)).release(any());
    }

    @Test
    void circuitOpensAfterRepeatedFailuresAndBlocksFurtherCalls() {
        when(client.getBySku("SKU-1001")).thenThrow(serverError());

        boolean opened = false;
        for (int i = 0; i < 5 && !opened; i++) {
            try {
                catalog.findBySku("SKU-1001");
            } catch (CallNotPermittedException e) {
                opened = true;
            } catch (InventoryUnavailableException e) {
                // still closed: keep going
            }
        }
        assertTrue(opened, "the circuit should open after repeated failures");

        clearInvocations(client);
        assertThrows(CallNotPermittedException.class, () -> catalog.findBySku("SKU-1001"));
        verifyNoInteractions(client);                       // an open circuit does not even call Inventory
    }

    @Test
    void openCircuitAlsoBlocksReserveBecauseTheBreakerIsShared() {
        circuitBreakerRegistry.circuitBreaker("inventory").transitionToOpenState();

        assertThrows(CallNotPermittedException.class, () -> reservation.reserve("ORD-1", lines));
        verifyNoInteractions(client);
    }
}