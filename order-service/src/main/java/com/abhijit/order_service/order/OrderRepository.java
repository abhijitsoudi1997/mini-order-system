package com.abhijit.order_service.order;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long>{
	
	Optional<Order> findByOrderNumber(String orderNumber);

	Page<Order> findByCustomerEmailIgnoreCase(String customerEmail, Pageable pageable);

}
