package com.in28minutes.springboot.learn_spring_boot.product;

public record ProductRequest(String sku,
        String name,
        String category,
        double price,
        int quantity) {
	
	

}
