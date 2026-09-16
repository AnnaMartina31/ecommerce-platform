package com.anna.ecommerce.ordersservice.order;

import com.anna.ecommerce.ordersservice.order.client.ProductClient;
import com.anna.ecommerce.ordersservice.order.client.UserClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class OrderControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private UserClient userClient;

    @MockitoBean
    private ProductClient productClient;

    @Test
    void createOrder_withValidUserAndProduct_returnsCreatedOrder() {
        when(userClient.userExists(1L)).thenReturn(true);
        when(productClient.getProduct(2L)).thenReturn(
                Optional.of(new ProductClient.ProductDto(2L, "Cuffie Bluetooth", new BigDecimal("89.90"), 10))
        );

        OrderController.CreateOrderRequest request = new OrderController.CreateOrderRequest(
                1L, List.of(new OrderController.OrderItemDto(2L, 2))
        );

        ResponseEntity<Order> response = restTemplate.postForEntity("/orders", request, Order.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTotalAmount()).isEqualByComparingTo("179.80");
    }

    @Test
    void createOrder_withNonExistentUser_returns409() {
        when(userClient.userExists(999L)).thenReturn(false);

        OrderController.CreateOrderRequest request = new OrderController.CreateOrderRequest(
                999L, List.of(new OrderController.OrderItemDto(2L, 1))
        );

        ResponseEntity<String> response = restTemplate.postForEntity("/orders", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}