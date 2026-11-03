package com.abhijit.order_service.catalog;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.abhijit.order_service.order.InsufficientStockException;
import com.abhijit.order_service.order.UnknownProductException;

@Component
@Profile("test")
public class StubStockReservation implements StockReservation{
	
	private final ProductCatalog productCatalog;
	
	public StubStockReservation(ProductCatalog productCatalog) {
		this.productCatalog = productCatalog;
	}

	@Override
	public void reserve(String orderNumber, List<StockLine> lines) {
		for (StockLine line : lines) {
            ProductInfo product = productCatalog.findBySku(line.sku())
                    .orElseThrow(() -> new UnknownProductException(line.sku()));
            if (product.availableQuantity() < line.quantity()) {
                throw new InsufficientStockException(line.sku(), line.quantity(), product.availableQuantity());
            }
        }
	}

	@Override
	public void release(String orderNumber, List<StockLine> lines) {
		// TODO Auto-generated method stub
		
	}

}
