package com.abhijit.order_service.order;

public class InsufficientStockException extends RuntimeException{
	public InsufficientStockException(String sku, int requested, int available) {
		super("Insufficient stock for " + sku + ":requested " + requested + ", available " + available);
	}
	
	public InsufficientStockException(String message) {
		super(message);
	}

}
