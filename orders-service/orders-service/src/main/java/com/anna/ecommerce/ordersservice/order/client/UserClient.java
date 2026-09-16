package com.anna.ecommerce.ordersservice.order.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;

@Component
public class UserClient {

    private final RestClient restClient;

    public UserClient(@Value("${users.service.url}") String usersServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(usersServiceUrl)
                .build();
    }

    public boolean userExists(Long userId) {
        try {
            restClient.get()
                    .uri("/users/{id}", userId)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }
}