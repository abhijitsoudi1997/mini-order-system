package com.abhijit.inventory_service.product;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "stock_reservation")
public class Reservation {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "order_number", nullable = false, unique = true)
	private String orderNumber;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ReservationStatus status = ReservationStatus.RESERVED;
	
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name="stock_reservation_line", joinColumns = @JoinColumn(name = "reservation_id"))
	private List<ReservedItem> items = new ArrayList<>();
	
	protected Reservation() {
		
	}

	public Reservation(String orderNumber, List<ReservedItem> items) {
		this.orderNumber = orderNumber;
		this.items = new ArrayList<>(items);
	}

	public Long getId() {
		return id;
	}

	public String getOrderNumber() {
		return orderNumber;
	}

	public ReservationStatus getStatus() {
		return status;
	}

	public List<ReservedItem> getItems() {
		return items;
	}
	
	
	public void markReleased() {
		this.status = ReservationStatus.RELEASED;
	}
	
	
}
