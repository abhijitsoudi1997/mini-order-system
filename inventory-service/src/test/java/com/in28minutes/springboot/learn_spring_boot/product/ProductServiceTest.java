package com.in28minutes.springboot.learn_spring_boot.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
	
	@Mock
    private ProductRepository repository;

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(repository, new ProductMapper());
    }

    
    @Test
    void findById_whenExists_returnsResponse() {
        Product laptop = new Product("SKU-1001", "Laptop", "Electronics", 55000, 10);
        when(repository.findById(1L)).thenReturn(Optional.of(laptop));

        Optional<ProductResponse> result = service.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("Laptop", result.get().name());
    }

    @Test
    void findById_whenMissing_returnsEmpty() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertTrue(service.findById(99L).isEmpty());
    }

    @Test
    void findAll_withCategory_usesCategoryQuery() {
    	Pageable pageable = PageRequest.of(0, 10);
    	Product desk = new Product("SKU-1003", "Desk", "Furniture", 8000, 5);
        when(repository.findByCategory("Furniture", pageable))
                .thenReturn(new PageImpl<>(List.of(desk), pageable, 1));

        Page<ProductResponse> result = service.findAll("Furniture", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Desk", result.getContent().get(0).name());
        verify(repository, never()).findAll(any(Pageable.class));
    }
    
    @Test
    void findBySku_whenMissing_returnsEmpty() {
        when(repository.findBySku("NOPE")).thenReturn(Optional.empty());

        assertTrue(service.findBySku("NOPE").isEmpty());
    }

    @Test
    void delete_whenMissing_returnsFalseAndDeletesNothing() {
        when(repository.existsById(99L)).thenReturn(false);

        boolean deleted = service.deleteProduct(99L);

        assertFalse(deleted);
        verify(repository, never()).deleteById(any());
    }

}
