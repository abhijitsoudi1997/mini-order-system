package com.abhijit.order_service.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.abhijit.order_service.order.InsufficientStockException;
import com.abhijit.order_service.order.InventoryUnavailableException;
import com.abhijit.order_service.order.StockReleaseRejectedException;
import com.abhijit.order_service.order.StockRequest;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;

@ExtendWith(MockitoExtension.class)
class InventoryStockReservationTest {

    @Mock
    private InventoryClient client;

    @InjectMocks
    private InventoryStockReservation reservation;

    private final List<StockLine> lines = List.of(new StockLine("SKU-1001", 2));

    private Request dummyRequest() {
        return Request.create(Request.HttpMethod.POST, "/inventory/reserve",
                Map.of(), null, StandardCharsets.UTF_8, new RequestTemplate());
    }

    @Test
    void reserveSendsOrderNumberAndItemsToInventory() {
        reservation.reserve("ORD-1", lines);

        ArgumentCaptor<StockRequest> captor = ArgumentCaptor.forClass(StockRequest.class);
        verify(client).reserve(captor.capture());
        assertEquals("ORD-1", captor.getValue().orderNumber());
        assertEquals("SKU-1001", captor.getValue().items().get(0).sku());
        assertEquals(2, captor.getValue().items().get(0).quantity());
    }

    @Test
    void conflictBecomesInsufficientStockWithInventoryMessage() {
        byte[] body = "{\"message\":\"Insufficient stock for SKU-1001 (requested 2)\"}".getBytes(StandardCharsets.UTF_8);
        doThrow(new FeignException.Conflict("conflict", dummyRequest(), body, null))
                .when(client).reserve(any());

        InsufficientStockException ex = assertThrows(InsufficientStockException.class,
                () -> reservation.reserve("ORD-1", lines));

        assertEquals("Insufficient stock for SKU-1001 (requested 2)", ex.getMessage());
    }

    @Test
    void notFoundBecomesInsufficientStock() {
        doThrow(new FeignException.NotFound("not found", dummyRequest(), null, null))
                .when(client).reserve(any());

        assertThrows(InsufficientStockException.class, () -> reservation.reserve("ORD-1", lines));
    }

    @Test
    void serverErrorBecomesInventoryUnavailable() {
        doThrow(new FeignException.InternalServerError("boom", dummyRequest(), null, null))
                .when(client).reserve(any());

        assertThrows(InventoryUnavailableException.class, () -> reservation.reserve("ORD-1", lines));
    }

    @Test
    void releaseSendsRequestToInventory() {
        reservation.release("ORD-1", lines);

        verify(client).release(any(StockRequest.class));
    }
    
    @Test
    void releaseOf404BecomesStockReleaseRejected() {
        doThrow(new FeignException.NotFound("not found", dummyRequest(), null, null))
                .when(client).release(any());

        assertThrows(StockReleaseRejectedException.class, () -> reservation.release("ORD-1", lines));
    }
}
