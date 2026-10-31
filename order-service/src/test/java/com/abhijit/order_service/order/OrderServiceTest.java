package com.abhijit.order_service.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.abhijit.order_service.catalog.StubProductCatalog;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {
	
	@Mock
    private OrderRepository repository;

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(repository, new OrderMapper(),
                new OrderNumberGenerator(), new StubProductCatalog());
    }

    private CreateOrderRequest request(OrderItemRequest... items) {
        return new CreateOrderRequest("Ravi Kumar", "ravi@example.com", null,
                new ShippingAddressRequest("12 MG Road", "Mysore", "Karnataka", "570001"),
                List.of(items));
    }

    @Test
    void createOrder_calculatesTotalAndSetsNumberAndStatus() {
        when(repository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = service.createOrder(
                request(new OrderItemRequest("SKU-1001", 2), new OrderItemRequest("SKU-1002", 1)));

        assertEquals(new BigDecimal("110700.00"), response.totalAmount());
        assertEquals(OrderStatus.CREATED, response.status());
        assertTrue(response.orderNumber().startsWith("ORD-"));
        assertEquals(2, response.items().size());
        assertEquals("Laptop", response.items().get(0).productName());
    }

    @Test
    void createOrder_withUnknownSku_throwsAndSavesNothing() {
        assertThrows(UnknownProductException.class,
                () -> service.createOrder(request(new OrderItemRequest("NOPE", 1))));

        verify(repository, never()).save(any());
    }

}
