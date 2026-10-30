package com.in28minutes.springboot.learn_spring_boot.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

	Page<Product> findByCategory(String category, Pageable pageable);

	Optional<Product> findBySku(String sku);

	Page<Product> findByActiveTrue(Pageable pageable);

	Page<Product> findByCategoryAndActiveTrue(String category, Pageable pageable);

	Optional<Product> findByIdAndActiveTrue(Long id);

	Optional<Product> findBySkuAndActiveTrue(String sku);

}
