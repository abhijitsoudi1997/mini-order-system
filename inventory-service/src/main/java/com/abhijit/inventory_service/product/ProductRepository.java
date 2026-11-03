package com.abhijit.inventory_service.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

	Page<Product> findByCategory(String category, Pageable pageable);

	Optional<Product> findBySku(String sku);

	Page<Product> findByActiveTrue(Pageable pageable);

	Page<Product> findByCategoryAndActiveTrue(String category, Pageable pageable);

	Optional<Product> findByIdAndActiveTrue(Long id);

	Optional<Product> findBySkuAndActiveTrue(String sku);
	
	@Modifying(clearAutomatically = true)
	@Query("update Product p set p.reservedQuantity = p.reservedQuantity + :qty "
		     + "where p.sku = :sku and p.active = true and (p.quantity - p.reservedQuantity) >= :qty")
	int reserve(@Param("sku") String sku, @Param("qty") int qty);
	
	@Modifying(clearAutomatically = true)
	@Query("update Product p set p.reservedQuantity = p.reservedQuantity - :qty "
		     + "where p.sku = :sku and p.active = true and p.reservedQuantity >= :qty")
	int release(@Param("sku") String sku, @Param("qty") int qty);
}
