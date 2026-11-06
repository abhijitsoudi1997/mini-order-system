package com.abhijit.inventory_service.product;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long>{
	
	Optional<Reservation> findByOrderNumber(String orderNumber);

}
