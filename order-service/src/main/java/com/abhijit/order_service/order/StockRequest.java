package com.abhijit.order_service.order;

import java.util.List;

public record StockRequest(String orderNumber, List<StockItemRequest> items) {

}
