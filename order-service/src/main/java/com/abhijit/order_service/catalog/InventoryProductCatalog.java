package com.abhijit.order_service.catalog;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.abhijit.order_service.order.InventoryUnavailableException;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Component
@Profile("!test")
public class InventoryProductCatalog implements ProductCatalog{

	private final InventoryClient inventoryClient;
	
	private static final Logger log = LoggerFactory.getLogger(InventoryProductCatalog.class);

	
	public InventoryProductCatalog(InventoryClient inventoryClient) {
		this.inventoryClient = inventoryClient;
	}
	
	@Override
	@Retry(name = "inventory")
	@CircuitBreaker(name = "inventory")
	public Optional<ProductInfo> findBySku(String sku) {

		
		try {
			InventoryProduct inventoryProduct = inventoryClient.getBySku(sku);
			return Optional.of(new ProductInfo(inventoryProduct.sku(), inventoryProduct.name(), inventoryProduct.price(), inventoryProduct.quantity()));
		} catch(FeignException.NotFound e) {
			return Optional.empty();
		}catch(FeignException e) {
			log.warn("Inventory call failed for {}: {}", sku, e.getMessage());
			throw new InventoryUnavailableException("Inventory Service is unavailable, please try again later", e);
		}
		
	}

}
