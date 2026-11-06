package com.abhijit.inventory_service.product;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory")
public class StockController {
	
	private final ReservationService reservationService;
	
	public StockController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}
	
	@PostMapping("/reserve")
	public StockResponse reserve(@Valid @RequestBody StockRequest request) {
		
		reservationService.reserve(request.orderNumber(), toStockItems(request));
		return new StockResponse(request.orderNumber(), "RESERVED", request.items());
	}
	
	@PostMapping("/release")
	public StockResponse release(@Valid @RequestBody StockRequest request) {
		
		reservationService.release(request.orderNumber());
		return new StockResponse(request.orderNumber(), "RELEASED", request.items());
	}
	
	private List<StockItem> toStockItems(StockRequest request) {
		return request.items().stream()
								.map(i -> new StockItem(i.sku(), i.quantity()))
								.toList();
	}

}
