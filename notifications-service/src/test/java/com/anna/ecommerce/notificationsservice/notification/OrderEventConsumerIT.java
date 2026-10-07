package com.anna.ecommerce.notificationsservice.notification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false"
})
class OrderEventConsumerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private OrderEventConsumer consumer;

    @Autowired
    private ProcessedEventRepository processedEvents;

    @Test
    void sameEventDeliveredTwice_isProcessedOnlyOnce() {
        String payload = "{\"orderId\":42,\"userId\":1,\"totalAmount\":10.00,"
                + "\"createdAt\":\"2026-10-07T10:00:00Z\"}";

        consumer.handleOrderCreated(payload);
        consumer.handleOrderCreated(payload); // duplicato (consegna at-least-once)

        assertThat(processedEvents.findAll())
                .filteredOn(e -> e.getOrderId().equals(42L))
                .hasSize(1);
    }

    @Test
    void markAsProcessed_returnsTrueFirstTimeAndFalseOnDuplicate() {
        assertThat(consumer.markAsProcessed(100L)).isTrue();
        assertThat(consumer.markAsProcessed(100L)).isFalse();
    }
}