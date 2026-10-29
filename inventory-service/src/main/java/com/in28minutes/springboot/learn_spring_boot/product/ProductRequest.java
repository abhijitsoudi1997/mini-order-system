package com.in28minutes.springboot.learn_spring_boot.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ProductRequest(
		@NotBlank(message = "sku is required")String sku,
		@NotBlank(message = "name is required")String name,
		@NotBlank(message = "category is required")String category,
		@Positive(message = "price must be greater than 0")double price,
		@Min(value = 0, message = "quantity cannot be negative")int quantity) {
	
	

}
