package com.abhijit.order_service.catalog;

import java.util.Optional;

public interface ProductCatalog {
	
	Optional<ProductInfo> findBySku(String sku);

}
