package com.abhijit.order_service.catalog;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.abhijit.order_service.order.StockRequest;

@FeignClient(name = "inventory-service", url = "${inventory.base-url}")
public interface InventoryClient {
	
	@GetMapping("/products/sku/{sku}")
    InventoryProduct getBySku(@PathVariable("sku") String sku);
	
	@PostMapping("/inventory/reserve")
	void reserve(@RequestBody StockRequest request);
	
	@PostMapping("/inventory/release")
	void release(@RequestBody StockRequest request);

}
