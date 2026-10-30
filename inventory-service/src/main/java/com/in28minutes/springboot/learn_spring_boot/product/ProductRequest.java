package com.in28minutes.springboot.learn_spring_boot.product;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductRequest(
		@Schema(description = "Unique stock keeping unit", example = "SKU-1006")
        @NotBlank(message = "sku is required") String sku,

        @Schema(description = "Product name", example = "Pen")
        @NotBlank(message = "name is required") String name,

        @Schema(description = "Product category", example = "Stationery")
        @NotBlank(message = "category is required") String category,

        @Schema(description = "Unit price, must be greater than 0", example = "20.0")
		@NotNull(message = "price is required")
        @Positive(message = "price must be greater than 0") BigDecimal price,

        @Schema(description = "Units in stock, 0 or more", example = "500")
        @Min(value = 0, message = "quantity cannot be negative") int quantity) {
	
	

}
