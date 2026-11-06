package com.abhijit.order_service.catalog;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.abhijit.order_service.order.InsufficientStockException;
import com.abhijit.order_service.order.InventoryUnavailableException;
import com.abhijit.order_service.order.StockItemRequest;
import com.abhijit.order_service.order.StockReleaseRejectedException;
import com.abhijit.order_service.order.StockRequest;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Component
@Profile("!test")
public class InventoryStockReservation implements StockReservation{
	
    private static final Logger log = LoggerFactory.getLogger(InventoryStockReservation.class);

	private static final Pattern MESSAGE = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");
	
	private final InventoryClient inventoryClient;
	
	public InventoryStockReservation(InventoryClient inventoryClient) {
		this.inventoryClient = inventoryClient;
	}

	@Override
	@Retry(name = "inventory")
	@CircuitBreaker(name = "inventory")
	public void reserve(String orderNumber, List<StockLine> lines) {
		try {
			inventoryClient.reserve(toRequest(orderNumber, lines));
		} catch(FeignException.Conflict | FeignException.NotFound e) {
			throw new InsufficientStockException(messageFrom(e, "Inventory could not reserve the requested stock"));
		} catch(FeignException e) {
            log.warn("Inventory reserve failed for order {}: {}", orderNumber, e.getMessage());
			throw new InventoryUnavailableException("Inventory service is unavailable, please try again later", e);
		}
	}

	@Override
	@Retry(name = "inventory")
	@CircuitBreaker(name = "inventory")
	public void release(String orderNumber, List<StockLine> lines) {
		try {
			inventoryClient.release(toRequest(orderNumber, lines));
		} catch (FeignException.NotFound e) {
            // Inventory has no reservation for this order: a business answer, not an outage. Not retried.
            throw new StockReleaseRejectedException("Inventory has no reservation for order " + orderNumber);
        } catch(FeignException e) {
            log.warn("Inventory release failed for order {}: {}", orderNumber, e.getMessage());
			throw new InventoryUnavailableException("Inventory service is unavailable, please try again later", e);
		}
	}
	
	private StockRequest toRequest(String orderNumber, List<StockLine> lines) {
		return new StockRequest(orderNumber, lines.stream().map(l -> new StockItemRequest(l.sku(), l.quantity())).toList());
	}
	
	private String messageFrom(FeignException e, String fallback) {
		Matcher m = MESSAGE.matcher(e.contentUTF8());
		return m.find() ? m.group(1) : fallback;
	}
	

}
