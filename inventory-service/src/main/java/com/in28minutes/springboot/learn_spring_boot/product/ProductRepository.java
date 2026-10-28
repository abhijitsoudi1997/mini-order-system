package com.in28minutes.springboot.learn_spring_boot.product;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long>{
	
	List<Product> findByCategory(String category);

}
