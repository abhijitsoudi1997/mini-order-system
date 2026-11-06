package com.abhijit.inventory_service.product;

public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(String orderNumber) {
        super("No reservation found for order " + orderNumber);
    }
}