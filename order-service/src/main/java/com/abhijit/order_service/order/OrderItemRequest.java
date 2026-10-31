package com.abhijit.order_service.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record OrderItemRequest(
		
		@NotBlank(message = "sku is required")String sku,
		@Positive(message = "quantity must be greater than 0")int quantity) {

}
