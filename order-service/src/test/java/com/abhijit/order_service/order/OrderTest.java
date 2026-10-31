package com.abhijit.order_service.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class OrderTest {
	
	@Test
    void addLine_updatesTotalAndLinksLineToOrder() {
        Order order = new Order();
        OrderLine laptop = new OrderLine("SKU-1001", "Laptop", 2, new BigDecimal("55000.00"));
        OrderLine mouse = new OrderLine("SKU-1002", "Mouse", 1, new BigDecimal("700.00"));

        order.addLine(laptop);
        order.addLine(mouse);

        assertEquals(new BigDecimal("110700.00"), order.getTotalAmount());
        assertSame(order, laptop.getOrder());
        assertEquals(2, order.getLines().size());
    }

}
