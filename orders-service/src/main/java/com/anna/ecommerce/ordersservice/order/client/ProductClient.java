package com.anna.ecommerce.ordersservice.order.client;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Component
public class ProductClient {

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;

    public ProductClient(@Value("${products.service.url}") String productsServiceUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(1));
        factory.setReadTimeout(Duration.ofSeconds(2));

        this.restClient = RestClient.builder()
                .baseUrl(productsServiceUrl)
                .requestFactory(factory)
                .build();

        this.circuitBreaker = CircuitBreaker.of("products-service", CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .ignoreExceptions(HttpClientErrorException.class)
                .build());
    }

    public record ProductDto(Long id, String name, BigDecimal price, Integer stockQuantity) {}

    public Optional<ProductDto> getProduct(Long productId) {
        try {
            return circuitBreaker.executeSupplier(() -> {
                try {
                    ProductDto product = restClient.get()
                            .uri("/products/{id}", productId)
                            .retrieve()
                            .body(ProductDto.class);
                    return Optional.ofNullable(product);
                } catch (HttpClientErrorException.NotFound e) {
                    return Optional.<ProductDto>empty();
                }
            });
        } catch (CallNotPermittedException e) {
            throw new DependencyUnavailableException("products-service non disponibile (circuito aperto)", e);
        } catch (HttpClientErrorException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new DependencyUnavailableException("products-service non raggiungibile", e);
        }
    }
}