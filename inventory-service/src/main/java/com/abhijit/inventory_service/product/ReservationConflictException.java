package com.abhijit.inventory_service.product;

public class ReservationConflictException extends RuntimeException {
    public ReservationConflictException(String message) { super(message); }
}