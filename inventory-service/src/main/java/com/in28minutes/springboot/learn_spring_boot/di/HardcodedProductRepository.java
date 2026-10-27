package com.in28minutes.springboot.learn_spring_boot.di;

import java.util.List;

import org.springframework.stereotype.Repository;

@Repository
public class HardcodedProductRepository implements ProductRepository{
	
	@Override
    public List<String> findAllNames() {
        return List.of("Chair", "Pen", "Notebook");
    }

}
