package com.in28minutes.springboot.learn_spring_boot.product;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
	
	private static final Logger log = LoggerFactory.getLogger(ProductService.class);
	
	private ProductRepository productRepository;
	private ProductMapper mapper;
	
	public ProductService(ProductRepository productRepository, ProductMapper mapper) {
		this.productRepository = productRepository;
		this.mapper = mapper;
	}
	
	

	public Page<ProductResponse> findAll(String category, Pageable pageable) {
		Page<Product> products = (category == null)
					? productRepository.findAll(pageable)
					:productRepository.findByCategory(category, pageable);
		
		return products.map(mapper::toResponse);

	}

	public Optional<ProductResponse> findById(Long id) {
		return productRepository.findById(id).map(mapper::toResponse);
	}

	public ProductResponse createProduct(ProductRequest request) {
		Product saved = productRepository.save(mapper.toEntity(request));
		return mapper.toResponse(saved);
	}

	public Optional<ProductResponse> updateProduct(Long id, ProductRequest request) {
		return productRepository.findById(id).map(existing -> {
	        existing.setSku(request.sku());
	        existing.setName(request.name());
	        existing.setCategory(request.category());
	        existing.setPrice(request.price());
	        existing.setQuantity(request.quantity());
	        return mapper.toResponse(productRepository.save(existing));
	    });
	}

	public boolean deleteProduct(Long id) {
		if (!productRepository.existsById(id)) {
	        return false;
	    }
		productRepository.deleteById(id);
	    return true;
	}

}
