package com.in28minutes.springboot.learn_spring_boot.product;

import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
	
	public Product toEntity(ProductRequest request) {
        return new Product(
                request.sku(),
                request.name(),
                request.category(),
                request.price(),
                request.quantity());
    }

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                product.getQuantity());
    }

}
