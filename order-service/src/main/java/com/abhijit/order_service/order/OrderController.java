package com.abhijit.order_service.order;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/orders")
public class OrderController {
	
	private final OrderService orderService;
	
	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}
	
	@PostMapping
	public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request){
		OrderResponse created = orderService.createOrder(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
												.path("/{id}")
												.buildAndExpand(created.id())
												.toUri();
		return ResponseEntity.created(location).body(created);
		
	}

}
