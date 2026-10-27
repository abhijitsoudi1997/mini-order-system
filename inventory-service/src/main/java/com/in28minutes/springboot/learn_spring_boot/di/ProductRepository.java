package com.in28minutes.springboot.learn_spring_boot.di;

import java.util.List;

public interface ProductRepository {
	
	List<String> findAllNames();

}
