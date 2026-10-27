package com.in28minutes.springboot.learn_spring_boot.di;

import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;

@Component
public class DemoRunner implements CommandLineRunner{
	
	private final ProductService service;
	private final PriceFormatter formatter;

    public DemoRunner(ProductService service, PriceFormatter formatter) {
    	this.service = service;
    	this.formatter = formatter;
    }

    @Override
    public void run(String... args) { 
    	System.out.println(service.getNames()); 
    	System.out.println(formatter.format(55000));
    }

}
