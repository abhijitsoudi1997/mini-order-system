package com.abhijit.order_service.order;

public class StockReleaseRejectedException extends RuntimeException {
    public StockReleaseRejectedException(String message) { super(message); }
}
