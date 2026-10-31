package com.abhijit.order_service.order;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class OrderMapper {
	
	public Order toEntity(CreateOrderRequest request) {
		Order order = new Order();
		order.setCustomerName(request.customerName());
		order.setCustomerEmail(request.customerEmail());
		order.setCustomerPhone(request.customerPhone());
		ShippingAddressRequest address = request.shippingAddress();
		order.setShippingAddressLine(address.line());
		order.setShippingCity(address.city());
		order.setShippingState(address.state());
		order.setShippingPincode(address.pincode());
		
		return order;
	}
	
	public OrderResponse toResponse(Order order) {
		ShippingAddressResponse address = new ShippingAddressResponse(
				order.getShippingAddressLine(), order.getShippingCity(),
				order.getShippingState(), order.getShippingPincode());
		List<OrderLineResponse> items = order.getLines().stream()
											.map(this::toLineResponse)
											.toList();
		return new OrderResponse(order.getId(), order.getOrderNumber(), order.getCustomerName(),
				order.getCustomerEmail(), order.getCustomerPhone(), address, order.getStatus(),
				order.getTotalAmount(), order.getFailureReason(), order.getCreatedAt(), items);
	}
	
	private OrderLineResponse toLineResponse(OrderLine line) {
		return new OrderLineResponse(line.getSku(), line.getProductName(), line.getQuantity(),
				line.getUnitPrice(), line.getLineTotal());
	}

}
