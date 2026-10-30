package com.in28minutes.springboot.learn_spring_boot.product;

public class ProductNotFoundException extends RuntimeException{
	
	public ProductNotFoundException(Long id) {
        super("Product not found with id " + id);
    }
	
	public ProductNotFoundException(String sku) {
	    super("Product not found with sku " + sku);
	}

}
