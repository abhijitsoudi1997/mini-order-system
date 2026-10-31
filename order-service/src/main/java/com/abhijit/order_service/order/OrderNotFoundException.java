package com.abhijit.order_service.order;

public class OrderNotFoundException extends RuntimeException{
	
	public OrderNotFoundException(Long id) {
        super("Order not found with id " + id);
    }

    public OrderNotFoundException(String orderNumber) {
    	super("Order not found with order number " + orderNumber);
    }

}
