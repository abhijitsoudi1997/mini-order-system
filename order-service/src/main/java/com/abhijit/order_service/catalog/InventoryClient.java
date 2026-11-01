package com.abhijit.order_service.catalog;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "inventory-service", url = "${inventory.base-url}")
public interface InventoryClient {
	
	@GetMapping("/products/sku/{sku}")
    InventoryProduct getBySku(@PathVariable("sku") String sku);

}
