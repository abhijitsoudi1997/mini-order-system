package com.abhijit.order_service.order;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

@Component
public class OrderNumberGenerator {
	
	private static final String CHARS = "ABCDEFGHJKLMNPQRSTWXYZ23456789";
	private final SecureRandom random = new SecureRandom();
	
	public String next() {
		String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
		StringBuilder suffix = new StringBuilder();
		for(int i=0;i<6;i++) {
			suffix.append(CHARS.charAt(random.nextInt(CHARS.length())));
		}
		return "ORD-" + date + "-" + suffix;
	}

}
