package com.abhijit.order_service.order;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
		int status,
        String error,
        String message,
        String path,
        List<String> details,
        LocalDateTime timestamp) {

}
