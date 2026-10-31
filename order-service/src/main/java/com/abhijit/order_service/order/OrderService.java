package com.abhijit.order_service.order;

import org.springframework.stereotype.Service;

import com.abhijit.order_service.catalog.ProductCatalog;
import com.abhijit.order_service.catalog.ProductInfo;

import jakarta.transaction.Transactional;

@Service
public class OrderService {
	
	private final OrderRepository orderRepository;
	private final OrderMapper mapper;
	private final OrderNumberGenerator numberGenerator;
	private final ProductCatalog productCatalog;
	
	public OrderService(OrderRepository orderRepository, OrderMapper mapper, 
			OrderNumberGenerator numberGenerator, ProductCatalog productCatalog) {
		this.orderRepository = orderRepository;
		this.mapper = mapper;
		this.numberGenerator = numberGenerator;
		this.productCatalog = productCatalog;
	}
	
	@Transactional
	public OrderResponse createOrder(CreateOrderRequest request) {
		Order order = mapper.toEntity(request);
		order.setOrderNumber(numberGenerator.next());
		
		for(OrderItemRequest item : request.items()) {
			ProductInfo product = productCatalog.findBySku(item.sku())
					.orElseThrow(() -> new UnknownProductException(item.sku()));
			order.addLine(new OrderLine(product.sku(), product.name(), item.quantity(), product.unitPrice()));
		}
		
		Order saved = orderRepository.save(order);
		return mapper.toResponse(saved);
	}

}
