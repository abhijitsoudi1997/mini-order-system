package com.abhijit.inventory_service.product;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record StockRequest(
		@NotBlank(message = "orderNumber is required")String orderNumber,
		@NotEmpty(message = "at least one item is required")List<@Valid StockItemRequest> items) {

}
