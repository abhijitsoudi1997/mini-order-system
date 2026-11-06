package com.abhijit.inventory_service.product;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

	private final ReservationRepository reservationRepository;
	private final ProductService productService;

	public ReservationService(ReservationRepository reservationRepository, ProductService productService) {
		this.reservationRepository = reservationRepository;
		this.productService = productService;
	}

	@Transactional
	public void reserve(String orderNumber, List<StockItem> items) {
		Optional<Reservation> existing = reservationRepository.findByOrderNumber(orderNumber);

		if (existing.isPresent()) {
			Reservation reservation = existing.get();
			if (reservation.getStatus() == ReservationStatus.RELEASED) {
				throw new ReservationConflictException(
						"Order " + orderNumber + " was already released and cannot be reserved again");
			}
			if(!totalsOfReserved(reservation.getItems()).equals(totalsOfRequested(items))) {
				throw new ReservationConflictException(
						"Order " + orderNumber + " already has a reservation with different items");
			}
			return;
		}
		productService.reserve(items);
		reservationRepository.save(new Reservation(orderNumber,
								items.stream().map(i -> new ReservedItem(i.sku(), i.quantity())).toList()));
	}
	
	@Transactional
	public void release(String orderNumber) {
		Reservation reservation = reservationRepository.findByOrderNumber(orderNumber)
				.orElseThrow(() -> new ReservationNotFoundException(orderNumber));
		
		if(reservation.getStatus() == ReservationStatus.RELEASED) {
			return;
		}
		productService.release(reservation.getItems().stream()
													.map(i -> new StockItem(i.sku(), i.quantity()))
													.toList());
		reservation.markReleased();
		reservationRepository.save(reservation);
	}
	
	// totals per SKU, so the same SKU listed twice compares equal to one line with the sum
    private Map<String, Integer> totalsOfRequested(List<StockItem> items) {
        return items.stream().collect(Collectors.toMap(StockItem::sku, StockItem::quantity, Integer::sum));
    }

    private Map<String, Integer> totalsOfReserved(List<ReservedItem> items) {
        return items.stream().collect(Collectors.toMap(ReservedItem::sku, ReservedItem::quantity, Integer::sum));
    }


}
