package com.abhijit.order_service.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.abhijit.order_service.catalog.StockReservation;
import com.abhijit.order_service.catalog.StubProductCatalog;



@ExtendWith(MockitoExtension.class)
class OrderServiceReservationTest {

    @Mock private OrderRepository orderRepository;
    @Mock private StockReservation stockReservation;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        // same construction as in OrderServiceTest: real mapper, real generator, StubProductCatalog,
        // but with the mocked stockReservation as the last argument
        orderService = new OrderService(orderRepository, new OrderMapper(), new OrderNumberGenerator(),
                new StubProductCatalog(), stockReservation);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // build the request exactly like your other order tests do (name, email, phone, address, one item)
    private CreateOrderRequest request(OrderItemRequest... items) { /* SKU-1001, quantity 1 */ 
    	return new CreateOrderRequest(
                "Abhijit Soudi",
                "abhijit@example.com",
                "9876543210",
                new ShippingAddressRequest("12 MG Road", "Mysore", "Karnataka", "570001"),
                List.of(items));
    }

    @Test
    void orderIsConfirmedWhenReservationSucceeds() {
        OrderResponse response = orderService.createOrder(request());

        assertEquals(OrderStatus.CONFIRMED, response.status());
        assertNull(response.failureReason());
    }

    @Test
    void orderIsFailedWhenInventoryRefusesTheStock() {
        doThrow(new InsufficientStockException("Insufficient stock for SKU-1001 (requested 1)"))
                .when(stockReservation).reserve(anyString(), anyList());

        OrderResponse response = orderService.createOrder(request());

        assertEquals(OrderStatus.FAILED, response.status());
        assertEquals("Insufficient stock for SKU-1001 (requested 1)", response.failureReason());
    }

    @Test
    void inventoryUnavailableIsNotSwallowed() {
        doThrow(new InventoryUnavailableException("down", new RuntimeException()))
                .when(stockReservation).reserve(anyString(), anyList());

        assertThrows(InventoryUnavailableException.class, () -> orderService.createOrder(request()));
    }
}
