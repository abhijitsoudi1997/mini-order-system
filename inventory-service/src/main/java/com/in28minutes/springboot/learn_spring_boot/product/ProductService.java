package com.in28minutes.springboot.learn_spring_boot.product;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

@Service
public class ProductService {
	private final List<Product> products = new ArrayList<>(
			List.of(new Product(1L, "SKU-1001", "Laptop", "Electronics", 55000, 10),
					new Product(2L, "SKU-1002", "Mouse", "Electronics", 700, 50),
					new Product(3L, "SKU-1003", "Desk", "Furniture", 8000, 5),
					new Product(4L, "SKU-1004", "Chair", "Furniture", 4500, 0),
					new Product(5L, "SKU-1005", "Notebook", "Stationery", 120, 200)));

	private final AtomicLong idCounter = new AtomicLong(5);

	public List<Product> findAll(String category) {
		if (category == null) {
			return products;
		}
		return products.stream().filter(p -> p.category().equals(category)).toList();

	}

	public Optional<Product> findById(Long id) {
		Optional<Product> optional = products.stream().filter(p -> p.id().equals(id)).findFirst();
		return optional;
	}

	public Product createProduct(Product product) {
		long newId = idCounter.incrementAndGet();
		Product saved = new Product(newId, product.sku(), product.name(), product.category(), product.price(),
				product.quantity());
		products.add(saved);
		return saved;
	}

	public Optional<Product> updateProduct(Long id, Product product) {
		for (int i = 0; i < products.size(); i++) {
			if (products.get(i).id().equals(id)) {
				Product updated = new Product(id, product.sku(), product.name(), product.category(), product.price(),
						product.quantity());
				products.set(i, updated);
				return Optional.of(updated);
			}
		}

		return Optional.empty();
	}

	public boolean deleteProduct(Long id) {

		return products.removeIf(p -> p.id().equals(id));
	}

}
