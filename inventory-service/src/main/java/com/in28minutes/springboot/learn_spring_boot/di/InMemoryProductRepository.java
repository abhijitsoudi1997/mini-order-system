package com.in28minutes.springboot.learn_spring_boot.di;

import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class InMemoryProductRepository implements ProductRepository{
	
	@Override
    public List<String> findAllNames() {
        return List.of("Laptop", "Mouse", "Desk");
    }

}
