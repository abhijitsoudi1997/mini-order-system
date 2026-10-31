package com.abhijit.order_service.catalog;

import java.math.BigDecimal;

public record ProductInfo(String sku, String name, BigDecimal unitPrice) {

}
