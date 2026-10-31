package com.abhijit.order_service.catalog;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class StubProductCatalog implements ProductCatalog{
	
	private final Map<String, ProductInfo> products = Map.of(
            "SKU-1001", new ProductInfo("SKU-1001", "Laptop", new BigDecimal("55000.00")),
            "SKU-1002", new ProductInfo("SKU-1002", "Mouse", new BigDecimal("700.00")),
            "SKU-1003", new ProductInfo("SKU-1003", "Desk", new BigDecimal("8000.00")),
            "SKU-1004", new ProductInfo("SKU-1004", "Chair", new BigDecimal("4500.00")),
            "SKU-1005", new ProductInfo("SKU-1005", "Notebook", new BigDecimal("120.00")));

	@Override
	public Optional<ProductInfo> findBySku(String sku) {
		
		return Optional.ofNullable(products.get(sku));
	}
	
	

}
