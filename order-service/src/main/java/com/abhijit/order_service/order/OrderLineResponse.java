package com.abhijit.order_service.order;

import java.math.BigDecimal;

public record OrderLineResponse(String sku, String productName, int quantity,
        						BigDecimal unitPrice, BigDecimal lineTotal) {

}
