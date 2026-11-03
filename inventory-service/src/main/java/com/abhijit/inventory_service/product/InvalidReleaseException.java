package com.abhijit.inventory_service.product;

public class InvalidReleaseException extends RuntimeException{
	
	public InvalidReleaseException(String sku, int requested) {
		super("Cannot release " + requested + " of " + sku + ": more than reserved");
	}

}
