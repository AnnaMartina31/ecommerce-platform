package com.anna.ecommerce.productsservice.product;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) {
        super("Prodotto non trovato con id: " + id);
    }
}