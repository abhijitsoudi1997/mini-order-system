package com.in28minutes.springboot.learn_spring_boot.product;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner{
	
	private final ProductRepository productRepository;

    public DataLoader(ProductRepository repository) {
        this.productRepository = repository;
    }

    @Override
    public void run(String... args) {
    	
    	if(productRepository.count() > 0) {
    		return;
    	}
    	productRepository.save(new Product("SKU-1001", "Laptop", "Electronics", new BigDecimal("55000.00"), 10));
    	productRepository.save(new Product("SKU-1002", "Mouse", "Electronics", new BigDecimal("700.00"), 50));
    	productRepository.save(new Product("SKU-1003", "Desk", "Furniture", new BigDecimal("8000.00"), 5));
    	productRepository.save(new Product("SKU-1004", "Chair", "Furniture", new BigDecimal("4500.00"), 0));
    	productRepository.save(new Product("SKU-1005", "Notebook", "Stationery", new BigDecimal("120.00"), 200));

    }

}
