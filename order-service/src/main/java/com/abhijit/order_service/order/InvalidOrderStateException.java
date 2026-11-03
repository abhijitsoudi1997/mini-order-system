package com.abhijit.order_service.order;

public class InvalidOrderStateException extends RuntimeException{
	
	public InvalidOrderStateException(Long id, OrderStatus status) {
		super("Order " + id + " cannot be cancelled because its status is " + status);
	}

}
