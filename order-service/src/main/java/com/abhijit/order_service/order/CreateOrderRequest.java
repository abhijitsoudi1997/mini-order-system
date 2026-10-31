package com.abhijit.order_service.order;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
		@NotBlank(message = "customerName is required") @Size(max = 100)String customerName,
		@NotBlank(message = "customerEmail is required")
		@Email(message = "customerEmail must be a valid email") @Size(max = 120) String customerEmail,
		@Size(max = 20)String customerPhone,
		@NotNull(message = "shippingAddress is required") @Valid ShippingAddressRequest shippingAddress,
		@NotEmpty(message = "at least one item is required") @Valid List<OrderItemRequest> items) {

}
