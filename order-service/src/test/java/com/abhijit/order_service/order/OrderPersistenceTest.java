package com.abhijit.order_service.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.transaction.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OrderPersistenceTest {
	
	@Autowired
    private OrderRepository repository;

    @Test
    void savesOrderWithItsLines() {
        Order order = new Order();
        order.setOrderNumber("ORD-TEST-0001");
        order.setCustomerName("Test Customer");
        order.setCustomerEmail("test@example.com");
        order.setShippingAddressLine("12 Test Street");
        order.setShippingCity("Mysuru");
        order.setShippingState("Karnataka");
        order.setShippingPincode("570001");
        order.addLine(new OrderLine("SKU-1001", "Laptop", 2, new BigDecimal("55000.00")));
        order.addLine(new OrderLine("SKU-1002", "Mouse", 1, new BigDecimal("700.00")));

        Order saved = repository.saveAndFlush(order);

        assertNotNull(saved.getId());
        assertEquals(2, saved.getLines().size());
        assertNotNull(saved.getLines().get(0).getId());
        assertEquals(new BigDecimal("110000.00"), saved.getLines().get(0).getLineTotal());
    }

}
