package com.in28minutes.springboot.outside;

import java.util.List;

import org.springframework.stereotype.Repository;

@Repository
public class ProductRepository {
	
	public List<String> findAllNames() { return List.of("Laptop", "Mouse", "Desk"); }

}
