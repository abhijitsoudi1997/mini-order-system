package com.in28minutes.springboot.learn_spring_boot.product;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.in28minutes.springboot.learn_spring_boot.config.AppProperties;

@RestController
public class ProductController {

	private final ProductService service;
	private final AppProperties properties;

	public ProductController(ProductService service, AppProperties properties) {
		this.service = service;
		this.properties = properties;
	}

	@GetMapping("/hello")
	public String hello() {
		return properties.getWelcomeMessage();
	}

	@GetMapping("/products")
	public List<ProductResponse> getProducts(@RequestParam(required = false) String category) {
		return service.findAll(category);
	}

	@GetMapping("/products/{id}")
	public ResponseEntity<ProductResponse> getProductsById(@PathVariable Long id) {
		return service.findById(id)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());

	}

	@PostMapping("/products")
	public ResponseEntity<ProductResponse> createProduct(@RequestBody ProductRequest request) {

		ProductResponse saved = service.createProduct(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(saved.id())
				.toUri();

		return ResponseEntity.created(location).body(saved);

	}

	@PutMapping("/products/{id}")
	public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id,
	                                                     @RequestBody ProductRequest request) {
	    return service.updateProduct(id, request)
	            .map(ResponseEntity::ok)
	            .orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/products/{id}")
	public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
		if (!service.deleteProduct(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id);
		}

		return ResponseEntity.noContent().build();
	}

}
