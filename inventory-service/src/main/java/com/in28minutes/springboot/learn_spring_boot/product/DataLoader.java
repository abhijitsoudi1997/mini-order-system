package com.in28minutes.springboot.learn_spring_boot.product;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner{
	
	private final ProductRepository repository;

    public DataLoader(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
    	
    	if(repository.count() > 0) {
    		return;
    	}
        repository.save(new Product("SKU-1001", "Laptop", "Electronics", 55000, 10));
        repository.save(new Product("SKU-1002", "Mouse", "Electronics", 700, 50));
        repository.save(new Product("SKU-1003", "Desk", "Furniture", 8000, 5));
        repository.save(new Product("SKU-1004", "Chair", "Furniture", 4500, 0));
        repository.save(new Product("SKU-1005", "Notebook", "Stationery", 120, 200));

    }

}
