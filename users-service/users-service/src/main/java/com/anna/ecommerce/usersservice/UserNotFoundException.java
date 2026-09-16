package com.anna.ecommerce.usersservice;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("Utente non trovato con id: " + id);
    }
}