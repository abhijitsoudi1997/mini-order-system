package com.abhijit.order_service.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
		 Long id,
	        String orderNumber,
	        String customerName,
	        String customerEmail,
	        String customerPhone,
	        ShippingAddressResponse shippingAddress,
	        OrderStatus status,
	        BigDecimal totalAmount,
	        String failureReason,
	        LocalDateTime createdAt,
	        List<OrderLineResponse> items) {

}
