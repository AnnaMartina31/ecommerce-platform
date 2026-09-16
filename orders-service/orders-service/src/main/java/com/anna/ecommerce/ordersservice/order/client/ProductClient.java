package com.anna.ecommerce.ordersservice.order.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(@Value("${products.service.url}") String productsServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(productsServiceUrl)
                .build();
    }

    public record ProductDto(Long id, String name, BigDecimal price, Integer stockQuantity) {}

    public Optional<ProductDto> getProduct(Long productId) {
        try {
            ProductDto product = restClient.get()
                    .uri("/products/{id}", productId)
                    .retrieve()
                    .body(ProductDto.class);
            return Optional.ofNullable(product);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }
}