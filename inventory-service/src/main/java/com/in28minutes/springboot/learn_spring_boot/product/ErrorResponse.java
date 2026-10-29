package com.in28minutes.springboot.learn_spring_boot.product;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
		int status,
        String error,
        String message,
        List<String> details,
        LocalDateTime timestamp) {

}
