package com.abhijit.inventory_service.product;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.abhijit.inventory_service.product.Product;
import com.abhijit.inventory_service.product.ProductMapper;
import com.abhijit.inventory_service.product.ProductRepository;
import com.abhijit.inventory_service.product.ProductResponse;
import com.abhijit.inventory_service.product.ProductService;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
	
	@Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, new ProductMapper());
    }

    
    @Test
    void findById_whenExists_returnsResponse() {
        Product laptop = new Product("SKU-1001", "Laptop", "Electronics", BigDecimal.valueOf(55000), 10);
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(laptop));

        Optional<ProductResponse> result = productService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("Laptop", result.get().name());
    }

    @Test
    void findById_whenMissing_returnsEmpty() {
        when(productRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertTrue(productService.findById(99L).isEmpty());
    }

    @Test
    void findAll_withCategory_usesCategoryQuery() {
    	Pageable pageable = PageRequest.of(0, 10);
    	Product desk = new Product("SKU-1003", "Desk", "Furniture", BigDecimal.valueOf(8000), 5);
        when(productRepository.findByCategoryAndActiveTrue("Furniture", pageable))
                .thenReturn(new PageImpl<>(List.of(desk), pageable, 1));

        Page<ProductResponse> result = productService.findAll("Furniture", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Desk", result.getContent().get(0).name());
        verify(productRepository, never()).findByActiveTrue(any(Pageable.class));
    }
    
    @Test
    void findBySku_whenMissing_returnsEmpty() {
        when(productRepository.findBySkuAndActiveTrue("NOPE")).thenReturn(Optional.empty());

        assertTrue(productService.findBySku("NOPE").isEmpty());
    }
    
    @Test
    void reserveSucceedsWhenRepositoryUpdatesOneRow() {
        when(productRepository.reserve("SKU-1001", 2)).thenReturn(1);

        assertDoesNotThrow(() -> productService.reserve(List.of(new StockItem("SKU-1001", 2))));
    }

    @Test
    void reserveThrowsInsufficientStockWhenNoRowUpdatedButProductExists() {
        when(productRepository.reserve("SKU-1004", 1)).thenReturn(0);
        when(productRepository.findBySkuAndActiveTrue("SKU-1004")).thenReturn(Optional.of(new Product()));

        assertThrows(InsufficientStockException.class,
            () -> productService.reserve(List.of(new StockItem("SKU-1004", 1))));
    }
    
    @Test
    void reserveThrowsNotFoundWhenSkuDoesNotExist() {
        when(productRepository.reserve("SKU-9999", 1)).thenReturn(0);
        when(productRepository.findBySkuAndActiveTrue("SKU-9999")).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
            () -> productService.reserve(List.of(new StockItem("SKU-9999", 1))));
    }

    @Test
    void releaseThrowsWhenReleasingMoreThanReserved() {
        when(productRepository.release("SKU-1001", 5)).thenReturn(0);
        when(productRepository.findBySkuAndActiveTrue("SKU-1001")).thenReturn(Optional.of(new Product()));

        assertThrows(InvalidReleaseException.class,
            () -> productService.release(List.of(new StockItem("SKU-1001", 5))));
    }

    @Test
    void delete_whenMissing_returnsFalseAndDeletesNothing() {
        when(productRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        boolean deleted = productService.deleteProduct(99L);

        assertFalse(deleted);
        verify(productRepository, never()).save(any());
    }
    
    @Test
    void delete_whenExists_marksInactiveAndReturnsTrue() {
        Product laptop = new Product("SKU-1001", "Laptop", "Electronics", BigDecimal.valueOf(55000), 10);
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(laptop));

        boolean deleted = productService.deleteProduct(1L);

        assertTrue(deleted);
        assertFalse(laptop.isActive());
        verify(productRepository).save(laptop);
    }

}
