package com.anna.ecommerce.ordersservice.order.event;

import com.anna.ecommerce.ordersservice.order.outbox.OutboxEvent;
import com.anna.ecommerce.ordersservice.order.outbox.OutboxEventRepository;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class OrderEventPublisher {

    private static final String TOPIC = "order-events";

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OrderEventPublisher(OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /** Va chiamato dentro la stessa transazione che salva l'ordine. */
    public void publishOrderCreated(OrderCreatedEvent event) {
        String payload = objectMapper.writeValueAsString(event);
        outboxRepository.save(new OutboxEvent(TOPIC, event.orderId().toString(), payload));
    }
}