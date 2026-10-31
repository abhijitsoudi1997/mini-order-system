package com.abhijit.order_service.order;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class OrderNumberGeneratorTest {
	
	private final OrderNumberGenerator generator = new OrderNumberGenerator();

    @Test
    void next_hasExpectedFormat() {
        assertTrue(generator.next().matches("ORD-\\d{8}-[A-Z0-9]{6}"));
    }

    @Test
    void next_isDifferentEachTime() {
        assertNotEquals(generator.next(), generator.next());
    }

}
