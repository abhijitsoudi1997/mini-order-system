package com.abhijit.inventory_service.product;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
		int status,
        String error,
        String message,
        List<String> details,
        LocalDateTime timestamp) {

}
