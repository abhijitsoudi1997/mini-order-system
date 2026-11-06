package com.abhijit.order_service.order;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.abhijit.order_service.catalog.StockReservation;
import com.abhijit.order_service.catalog.InventoryStockReservation;
import com.abhijit.order_service.catalog.ProductCatalog;
import com.abhijit.order_service.catalog.ProductInfo;
import com.abhijit.order_service.catalog.StockLine;

import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
	
	private final OrderRepository orderRepository;
	private final OrderMapper mapper;
	private final OrderNumberGenerator numberGenerator;
	private final ProductCatalog productCatalog;
	private final StockReservation stockReservation;
	
    private static final Logger log = LoggerFactory.getLogger(InventoryStockReservation.class);

	
	public OrderService(OrderRepository orderRepository, OrderMapper mapper, 
			OrderNumberGenerator numberGenerator, ProductCatalog productCatalog, StockReservation stockReservation) {
		this.orderRepository = orderRepository;
		this.mapper = mapper;
		this.numberGenerator = numberGenerator;
		this.productCatalog = productCatalog;
		this.stockReservation = stockReservation;
	}
	
	@Transactional(readOnly = true)
	public Optional<OrderResponse> findById(Long id) {
	    return orderRepository.findById(id).map(mapper::toResponse);
	}

	@Transactional(readOnly = true)
	public Optional<OrderResponse> findByOrderNumber(String orderNumber) {
	    return orderRepository.findByOrderNumber(orderNumber).map(mapper::toResponse);
	}

	@Transactional(readOnly = true)
	public Page<OrderResponse> findByCustomerEmail(String customerEmail, Pageable pageable) {
	    return orderRepository.findByCustomerEmailIgnoreCase(customerEmail, pageable).map(mapper::toResponse);
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
		
		List<StockLine> lines = request.items().stream()
									.map(i -> new StockLine(i.sku(), i.quantity())).toList();
		
		try {
			stockReservation.reserve(saved.getOrderNumber(), lines);
			saved.confirm();
		} catch(InsufficientStockException e) {
			saved.fail(e.getMessage());
		} catch (InventoryUnavailableException e) {
		    // every retry failed, but Inventory may have reserved before the reply was lost:
		    // try to give the stock back, then fail the request (the order is rolled back)
		    releaseQuietly(saved.getOrderNumber(), lines);
		    throw e;
		}
		
		
		return mapper.toResponse(saved);
	}
	
	@Transactional
	public OrderResponse cancelOrder(Long id){
		Order order = orderRepository.findById(id)
						.orElseThrow(() -> new OrderNotFoundException(id));
		
		if(order.getStatus() != OrderStatus.CONFIRMED) {
			throw new InvalidOrderStateException(id, order.getStatus());
		}
		
		List<StockLine> lines = order.getLines().stream()
												.map(l -> new StockLine(l.getSku(), l.getQuantity()))
												.toList();
		
		stockReservation.release(order.getOrderNumber(), lines);
		order.cancel();
		
		return mapper.toResponse(order);
	}
	
	private void releaseQuietly(String orderNumber, List<StockLine> lines) {
	    try {
	        stockReservation.release(orderNumber, lines);
	    } catch (RuntimeException ex) {
	        log.warn("Could not release stock for order {} after a failed reserve: {}", orderNumber, ex.getMessage());
	    }
	}

}
