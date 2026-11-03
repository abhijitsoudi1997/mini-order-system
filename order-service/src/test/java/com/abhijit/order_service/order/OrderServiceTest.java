package com.abhijit.order_service.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.abhijit.order_service.catalog.StockLine;
import com.abhijit.order_service.catalog.StockReservation;
import com.abhijit.order_service.catalog.StubProductCatalog;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StockReservation stockReservation;


    @Captor
    private ArgumentCaptor<List<StockLine>> linesCaptor;

    private OrderService service;

    @BeforeEach
    void setUp() {
        // real mapper, real number generator and the stub catalog; only the repository
        // and the stock reservation are mocks
        service = new OrderService(orderRepository, new OrderMapper(), new OrderNumberGenerator(),
                new StubProductCatalog(), stockReservation);

        // save() returns the same order it was given (lenient: not every test reaches save)
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private CreateOrderRequest request(OrderItemRequest... items) {
        return new CreateOrderRequest(
                "Abhijit Soudi",
                "abhijit@example.com",
                "9876543210",
                new ShippingAddressRequest("12 MG Road", "Mysore", "Karnataka", "570001"),
                List.of(items));
    }

    @Test
    void orderIsConfirmedWhenReservationSucceeds() {
        OrderResponse response = service.createOrder(request(new OrderItemRequest("SKU-1001", 1)));

        assertEquals(OrderStatus.CONFIRMED, response.status());
        assertNull(response.failureReason());
    }

    @Test
    void orderLinesSnapshotNameAndPriceAndTotalIsCalculated() {
        OrderResponse response = service.createOrder(request(
                new OrderItemRequest("SKU-1001", 1),      // Laptop 55000.00
                new OrderItemRequest("SKU-1002", 2)));    // Mouse 700.00 x 2 = 1400.00

        assertEquals(2, response.items().size());
        assertEquals(new BigDecimal("56400.00"), response.totalAmount());
    }

    @Test
    void orderNumberIsGeneratedAndOrderIsSaved() {
        OrderResponse response = service.createOrder(request(new OrderItemRequest("SKU-1001", 1)));

        assertTrue(response.orderNumber().startsWith("ORD-"));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void reservationIsCalledWithOrderNumberAndRequestedLines() {
        OrderResponse response = service.createOrder(request(
                new OrderItemRequest("SKU-1001", 1),
                new OrderItemRequest("SKU-1002", 2)));

        verify(stockReservation).reserve(org.mockito.ArgumentMatchers.eq(response.orderNumber()), linesCaptor.capture());
        List<StockLine> lines = linesCaptor.getValue();
        assertEquals(2, lines.size());
        assertEquals("SKU-1001", lines.get(0).sku());
        assertEquals(1, lines.get(0).quantity());
        assertEquals("SKU-1002", lines.get(1).sku());
        assertEquals(2, lines.get(1).quantity());
    }

    @Test
    void orderIsFailedWhenInventoryRefusesTheStock() {
        doThrow(new InsufficientStockException("Insufficient stock for SKU-1001 (requested 1)"))
                .when(stockReservation).reserve(anyString(), anyList());

        OrderResponse response = service.createOrder(request(new OrderItemRequest("SKU-1001", 1)));

        assertEquals(OrderStatus.FAILED, response.status());
        assertEquals("Insufficient stock for SKU-1001 (requested 1)", response.failureReason());
    }

    @Test
    void inventoryUnavailableIsNotSwallowed() {
        doThrow(new InventoryUnavailableException("down", new RuntimeException()))
                .when(stockReservation).reserve(anyString(), anyList());

        assertThrows(InventoryUnavailableException.class,
                () -> service.createOrder(request(new OrderItemRequest("SKU-1001", 1))));
    }

    @Test
    void unknownSkuIsRejectedBeforeAnythingIsReserved() {
        assertThrows(UnknownProductException.class,
                () -> service.createOrder(request(new OrderItemRequest("SKU-9999", 1))));

        verify(stockReservation, never()).reserve(anyString(), anyList());
        verify(orderRepository, never()).save(any(Order.class));
    }
}