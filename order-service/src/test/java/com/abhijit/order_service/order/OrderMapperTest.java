package com.abhijit.order_service.order;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

public class OrderMapperTest {
	
	private final OrderMapper mapper = new OrderMapper();

    @Test
    void toEntity_copiesCustomerAndAddress() {
        CreateOrderRequest request = new CreateOrderRequest("Ravi Kumar", "ravi@example.com", null,
                new ShippingAddressRequest("12 MG Road", "Mysore", "Karnataka", "570001"),
                List.of(new OrderItemRequest("SKU-1001", 2)));

        Order order = mapper.toEntity(request);

        assertEquals("Ravi Kumar", order.getCustomerName());
        assertEquals("12 MG Road", order.getShippingAddressLine());
        assertEquals("570001", order.getShippingPincode());
        assertEquals(OrderStatus.CREATED, order.getStatus());
    }

    @Test
    void toResponse_includesLinesAndTotal() {
        Order order = new Order();
        order.setOrderNumber("ORD-20261031-ABCDEF");
        order.addLine(new OrderLine("SKU-1001", "Laptop", 2, new BigDecimal("55000.00")));

        OrderResponse response = mapper.toResponse(order);

        assertEquals("ORD-20261031-ABCDEF", response.orderNumber());
        assertEquals(1, response.items().size());
        assertEquals(new BigDecimal("110000.00"), response.totalAmount());
    }

}
