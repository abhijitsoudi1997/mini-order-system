package com.abhijit.order_service.order;

import java.net.URI;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.data.domain.Sort;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/orders")
public class OrderController {
	
	private final OrderService orderService;
	
	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}
	
	@GetMapping("/{id}")
	public OrderResponse getOrder(@PathVariable Long id) {
	    return orderService.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
	}

	@GetMapping("/number/{orderNumber}")
	public OrderResponse getOrderByNumber(@PathVariable String orderNumber) {
	    return orderService.findByOrderNumber(orderNumber)
	            .orElseThrow(() -> new OrderNotFoundException(orderNumber));
	}

	@GetMapping
	public PagedModel<OrderResponse> listOrders(
	        @RequestParam String customerEmail,
	        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
	    return new PagedModel<>(orderService.findByCustomerEmail(customerEmail, pageable));
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
