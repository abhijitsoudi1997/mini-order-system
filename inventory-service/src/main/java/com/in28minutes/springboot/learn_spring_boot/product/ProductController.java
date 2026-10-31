package com.in28minutes.springboot.learn_spring_boot.product;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
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

	private final ProductService productService;
	private final AppProperties appProperties;

	public ProductController(ProductService service, AppProperties properties) {
		this.productService = service;
		this.appProperties = properties;
	}

	@GetMapping("/hello")
	public String hello() {
		return appProperties.getWelcomeMessage();
	}

	@Operation(summary = "List products, optionally filtered by category")
	@ApiResponse(responseCode = "200", description = "List of products")
	@GetMapping("/products")
	public PagedModel<ProductResponse> getProducts(@RequestParam(required = false) String category,
													@ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
		return new PagedModel<>(productService.findAll(category, pageable));
	}

	@Operation(summary = "Get one product by id")
	@ApiResponse(responseCode = "200", description = "Product found")
	@ApiResponse(responseCode = "404", description = "Product not found")
	@GetMapping("/products/{id}")
	public ProductResponse getProductsById(@PathVariable Long id) {
		return productService.findById(id)
	            .orElseThrow(() -> new ProductNotFoundException(id));

	}
	
	@Operation(summary = "Get one product by SKU")
	@ApiResponse(responseCode = "200", description = "Product found")
	@ApiResponse(responseCode = "404", description = "Product not found")
	@GetMapping("/products/sku/{sku}")
	public ProductResponse getProductBySku(@PathVariable String sku) {
		return productService.findBySku(sku)
					.orElseThrow(() -> new ProductNotFoundException(sku));
	}

	@Operation(summary = "Create a product")
	@ApiResponse(responseCode = "201", description = "Product created")
	@ApiResponse(responseCode = "400", description = "Validation failed")
	@ApiResponse(responseCode = "409", description = "Sku already exists")
	@PostMapping("/products")
	public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {

		ProductResponse saved = productService.createProduct(request);
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
		return productService.updateProduct(id, request)
	            .orElseThrow(() -> new ProductNotFoundException(id));
	}

	@Operation(summary = "Soft delete a product")
	@ApiResponse(responseCode = "204", description = "Product deleted")
	@ApiResponse(responseCode = "404", description = "Product not found")
	@DeleteMapping("/products/{id}")
	public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
		if (!productService.deleteProduct(id)) {
	        throw new ProductNotFoundException(id);
	    }
	    return ResponseEntity.noContent().build();
	}

}
