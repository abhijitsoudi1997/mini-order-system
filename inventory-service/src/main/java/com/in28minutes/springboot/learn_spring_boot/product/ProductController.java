package com.in28minutes.springboot.learn_spring_boot.product;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ProductController {
	
	private final ProductService service;
	
	public ProductController(ProductService service) {
		this.service = service;
	}
	
	@GetMapping("/hello")
	public String hello() {
		return "Hello world";
	}
	
	@GetMapping("/products")
	public List<Product> getProducts(@RequestParam(required = false) String category) {
		return service.findAll(category);
	}
	
	@GetMapping("/products/{id}")
	public Product getProductsById(@PathVariable Long id) {
		Product product = service.findById(id)
										.orElseThrow(() -> 
										new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id));
		return product;
		
	}

}
