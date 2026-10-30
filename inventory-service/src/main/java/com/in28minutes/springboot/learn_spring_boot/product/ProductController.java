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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Products", description = "Manage the product catalogue")
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

	@Operation(summary = "List products, optionally filtered by category")
	@ApiResponse(responseCode = "200", description = "List of products")
	@GetMapping("/products")
	public List<ProductResponse> getProducts(@RequestParam(required = false) String category) {
		return service.findAll(category);
	}

	@Operation(summary = "Get one product by id")
	@ApiResponse(responseCode = "200", description = "Product found")
	@ApiResponse(responseCode = "404", description = "Product not found")
	@GetMapping("/products/{id}")
	public ProductResponse getProductsById(@PathVariable Long id) {
		return service.findById(id)
	            .orElseThrow(() -> new ProductNotFoundException(id));

	}

	@Operation(summary = "Create a product")
	@ApiResponse(responseCode = "201", description = "Product created")
	@ApiResponse(responseCode = "400", description = "Validation failed")
	@ApiResponse(responseCode = "409", description = "Sku already exists")
	@PostMapping("/products")
	public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {

		ProductResponse saved = service.createProduct(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(saved.id())
				.toUri();

		return ResponseEntity.created(location).body(saved);

	}

	@Operation(summary = "Update a product")
	@ApiResponse(responseCode = "200", description = "Product updated")
	@ApiResponse(responseCode = "400", description = "Validation failed")
	@ApiResponse(responseCode = "404", description = "Product not found")
	@PutMapping("/products/{id}")
	public ProductResponse updateProduct(@PathVariable Long id,
	                                                     @Valid @RequestBody ProductRequest request) {
		return service.updateProduct(id, request)
	            .orElseThrow(() -> new ProductNotFoundException(id));
	}

	@Operation(summary = "Delete a product")
	@ApiResponse(responseCode = "204", description = "Product deleted")
	@ApiResponse(responseCode = "404", description = "Product not found")
	@DeleteMapping("/products/{id}")
	public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
		if (!service.deleteProduct(id)) {
	        throw new ProductNotFoundException(id);
	    }
	    return ResponseEntity.noContent().build();
	}

}
