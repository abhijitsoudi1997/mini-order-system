package com.abhijit.order_service.catalog;

import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.abhijit.order_service.order.InventoryUnavailableException;

import feign.FeignException;

@Component
@Profile("!test")
public class InventoryProductCatalog implements ProductCatalog{

	private final InventoryClient inventoryClient;
	
	public InventoryProductCatalog(InventoryClient inventoryClient) {
		this.inventoryClient = inventoryClient;
	}
	
	@Override
	public Optional<ProductInfo> findBySku(String sku) {

		
		try {
			InventoryProduct inventoryProduct = inventoryClient.getBySku(sku);
			return Optional.of(new ProductInfo(inventoryProduct.sku(), inventoryProduct.name(), inventoryProduct.price(), inventoryProduct.quantity()));
		} catch(FeignException.NotFound e) {
			return Optional.empty();
		}catch(FeignException e) {
			throw new InventoryUnavailableException("Inventory Service is unavailable, please try again later", e);
		}
		
	}

}
