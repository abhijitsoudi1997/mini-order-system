package com.abhijit.order_service.order;

public class UnknownProductException extends RuntimeException{
	
	public UnknownProductException(String sku) {
		super("Unknown product sku " + sku);
	}

}
