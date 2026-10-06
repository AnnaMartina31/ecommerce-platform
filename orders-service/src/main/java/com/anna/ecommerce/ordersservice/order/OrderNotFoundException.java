package com.anna.ecommerce.ordersservice.order;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long id) {
        super("Ordine non trovato con id: " + id);
    }
}