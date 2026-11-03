package com.abhijit.inventory_service.product;

public class InsufficientStockException extends RuntimeException{
	
	public InsufficientStockException(String sku, int requested) {
		super("Insufficient stock for " + sku + " (requested " + requested + ")");
	}

}
