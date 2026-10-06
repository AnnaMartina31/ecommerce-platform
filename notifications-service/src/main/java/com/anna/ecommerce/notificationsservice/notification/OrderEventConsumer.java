package com.anna.ecommerce.notificationsservice.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final ObjectMapper objectMapper;

    public OrderEventConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order-events", groupId = "notifications-service")
    public void handleOrderCreated(String payload) {
        OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);

        log.info("📧 Notifica: nuovo ordine #{} creato dall'utente {} per un totale di {}€",
                event.orderId(), event.userId(), event.totalAmount());

        // Qui in futuro potresti: inviare una vera email, una push notification,
        // scrivere su un altro database di notifiche, ecc.
    }
}