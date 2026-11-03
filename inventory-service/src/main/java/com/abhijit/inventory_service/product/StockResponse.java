package com.abhijit.inventory_service.product;

import java.util.List;

public record StockResponse(String orderNumber, String status, List<StockItemRequest> items) {

}
