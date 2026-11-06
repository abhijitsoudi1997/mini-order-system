package com.abhijit.inventory_service.product;

import jakarta.persistence.Embeddable;

@Embeddable
public record ReservedItem(String sku, int quantity) {

}
