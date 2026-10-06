package com.anna.ecommerce.ordersservice.order;

import com.anna.ecommerce.ordersservice.order.client.ProductClient;
import com.anna.ecommerce.ordersservice.order.client.UserClient;
import com.anna.ecommerce.ordersservice.order.outbox.OutboxEvent;
import com.anna.ecommerce.ordersservice.order.outbox.OutboxEventRepository;
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
class OrderOutboxIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @MockitoBean
    private UserClient userClient;

    @MockitoBean
    private ProductClient productClient;

    @Test
    void createOrder_withKafkaDown_savesOrderAndLeavesEventPending() {
        when(userClient.userExists(1L)).thenReturn(true);
        when(productClient.getProduct(2L)).thenReturn(
                Optional.of(new ProductClient.ProductDto(2L, "Cuffie Bluetooth", new BigDecimal("89.90"), 10))
        );

        OrderController.CreateOrderRequest request = new OrderController.CreateOrderRequest(
                1L, List.of(new OrderController.OrderItemDto(2L, 2))
        );

        // Nessun Kafka in ascolto: la richiesta deve riuscire lo stesso.
        ResponseEntity<Order> response = restTemplate.postForEntity("/orders", request, Order.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long orderId = response.getBody().getId();

        List<OutboxEvent> events = outboxRepository.findAll().stream()
                .filter(e -> e.getEventKey().equals(orderId.toString()))
                .toList();

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getPublishedAt()).isNull();
        assertThat(events.get(0).getTopic()).isEqualTo("order-events");
    }
}