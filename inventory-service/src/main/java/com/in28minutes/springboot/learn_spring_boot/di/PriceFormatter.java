package com.in28minutes.springboot.learn_spring_boot.di;

public class PriceFormatter {
	
	private final String currency;

    public PriceFormatter(String currency) {
        this.currency = currency;
    }

    public String format(double price) {
        return currency + " " + price;
    }

}
