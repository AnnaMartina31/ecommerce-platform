package com.anna.ecommerce.notificationsservice.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEvents;

    public OrderEventConsumer(ObjectMapper objectMapper, ProcessedEventRepository processedEvents) {
        this.objectMapper = objectMapper;
        this.processedEvents = processedEvents;
    }

    @KafkaListener(topics = "order-events", groupId = "notifications-service")
    public void handleOrderCreated(String payload) {
        OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);

        if (!markAsProcessed(event.orderId())) {
            log.info("Evento per l'ordine #{} già gestito, duplicato ignorato", event.orderId());
            return;
        }

        sendNotification(event);
    }

    /** true se è la prima volta che vediamo questo ordine, false se è un duplicato. */
    boolean markAsProcessed(Long orderId) {
        try {
            processedEvents.saveAndFlush(new ProcessedEvent(orderId));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    void sendNotification(OrderCreatedEvent event) {
        log.info("Notifica: nuovo ordine #{} creato dall'utente {} per un totale di {} EUR",
                event.orderId(), event.userId(), event.totalAmount());
    }
}