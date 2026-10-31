package com.abhijit.order_service.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShippingAddressRequest(
		@NotBlank(message = "line is required") @Size(max=200)String line,
		@NotBlank(message = "city is required") @Size(max=80)String city,
		@NotBlank(message = "state is required") @Size(max=80)String state,
		@NotBlank(message = "pincode is required") 
		@Pattern(regexp = "\\d{6}", message = "pincode must be 6 digits")String pincode) {
}
