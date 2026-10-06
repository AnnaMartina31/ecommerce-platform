package com.anna.ecommerce.ordersservice.order.client;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class UserClient {

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;

    public UserClient(@Value("${users.service.url}") String usersServiceUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(1));
        factory.setReadTimeout(Duration.ofSeconds(2));

        this.restClient = RestClient.builder()
                .baseUrl(usersServiceUrl)
                .requestFactory(factory)
                .build();

        this.circuitBreaker = CircuitBreaker.of("users-service", CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .ignoreExceptions(HttpClientErrorException.class)
                .build());
    }

    public boolean userExists(Long userId) {
        try {
            return circuitBreaker.executeSupplier(() -> {
                try {
                    restClient.get()
                            .uri("/users/{id}", userId)
                            .retrieve()
                            .toBodilessEntity();
                    return true;
                } catch (HttpClientErrorException.NotFound e) {
                    return false;
                }
            });
        } catch (CallNotPermittedException e) {
            throw new DependencyUnavailableException("users-service non disponibile (circuito aperto)", e);
        } catch (HttpClientErrorException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new DependencyUnavailableException("users-service non raggiungibile", e);
        }
    }
}