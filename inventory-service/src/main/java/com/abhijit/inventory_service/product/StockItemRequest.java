package com.abhijit.inventory_service.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record StockItemRequest(
		@NotBlank(message = "sku is required")String sku,
		@Positive(message = "quantity must be greater than 0")int quantity) {

}
