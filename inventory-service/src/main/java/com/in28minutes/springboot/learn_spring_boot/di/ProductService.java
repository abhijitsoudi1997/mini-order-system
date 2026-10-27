package com.in28minutes.springboot.learn_spring_boot.di;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ProductService {
	
	private final ProductRepository repository;

    public ProductService(ProductRepository repository) {   // constructor injection
        this.repository = repository;
    }
    public List<String> getNames() { return repository.findAllNames(); }

}
