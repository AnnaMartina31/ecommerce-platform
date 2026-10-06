package com.anna.ecommerce.productsservice.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ProductControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7")).withExposedPorts(6379);
    
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createProduct_thenGetProduct_returnsCreatedProduct() {
        ProductController.CreateProductRequest request =
                new ProductController.CreateProductRequest(
                        "Mouse wireless", "Mouse ergonomico", new BigDecimal("29.90"), 50
                );

        ResponseEntity<Product> createResponse =
                restTemplate.postForEntity("/products", request, Product.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        Long newProductId = createResponse.getBody().getId();

        ResponseEntity<Product> getResponse =
                restTemplate.getForEntity("/products/" + newProductId, Product.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getName()).isEqualTo("Mouse wireless");
    }

    @Test
    void getProduct_withNonExistentId_returns404() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/products/999999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}