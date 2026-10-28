package com.in28minutes.springboot.learn_spring_boot.product;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
	
	private static final Logger log = LoggerFactory.getLogger(ProductService.class);
	
	private ProductRepository productRepository;
	
	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}
	
	

	public List<Product> findAll(String category) {
		if (category == null) {
			return productRepository.findAll();
		}
		log.debug("Fetching all products, category={}", category);
		return productRepository.findByCategory(category);

	}

	public Optional<Product> findById(Long id) {
		return productRepository.findById(id);
	}

	public Product createProduct(Product product) {
		return productRepository.save(product);
	}

	public Optional<Product> updateProduct(Long id, Product product) {
		return productRepository.findById(id).map(existing -> {
	        existing.setSku(product.getSku());
	        existing.setName(product.getName());
	        existing.setCategory(product.getCategory());
	        existing.setPrice(product.getPrice());
	        existing.setQuantity(product.getQuantity());
	        return productRepository.save(existing);
	    });
	}

	public boolean deleteProduct(Long id) {
		if (productRepository.existsById(id)) {
			productRepository.deleteById(id);
	        return true;
	    }
	    return false;
	}

}
