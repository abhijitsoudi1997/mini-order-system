package com.abhijit.order_service.catalog;

import java.util.List;

public interface StockReservation {
	
	void reserve(String orderNumber, List<StockLine> lines);
	void release(String orderNumber, List<StockLine> lines);

}
