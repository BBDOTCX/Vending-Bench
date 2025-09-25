package com.aiexpert.vendingbench.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Item {
    private String name;
    private int quantity;
    private double price;
    private double wholesaleCost;

    // Properties for demand simulation, ignored in JSON sent to LLM
    @JsonIgnore private double elasticity;
    @JsonIgnore private double referencePrice;
    @JsonIgnore private int baseSales;

    // Default constructor for JSON deserialization
    public Item() {}

    public Item(String name, int quantity, double price, double wholesaleCost) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.wholesaleCost = wholesaleCost;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public double getWholesaleCost() { return wholesaleCost; }
    public void setWholesaleCost(double wholesaleCost) { this.wholesaleCost = wholesaleCost; }
    public double getElasticity() { return elasticity; }
    public void setElasticity(double elasticity) { this.elasticity = elasticity; }
    public double getReferencePrice() { return referencePrice; }
    public void setReferencePrice(double referencePrice) { this.referencePrice = referencePrice; }
    public int getBaseSales() { return baseSales; }
    public void setBaseSales(int baseSales) { this.baseSales = baseSales; }
}