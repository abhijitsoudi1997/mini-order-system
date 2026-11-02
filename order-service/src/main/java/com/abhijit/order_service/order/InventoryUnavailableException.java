package com.abhijit.order_service.order;

public class InventoryUnavailableException extends RuntimeException{
	
	public InventoryUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}

}
