package com.anna.ecommerce.ordersservice.order.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelay(OutboxEventRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    public void publishPending() {
        for (OutboxEvent event : repository.findTop50ByPublishedAtIsNullOrderByIdAsc()) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getEventKey(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);
                event.markPublished();
                repository.save(event);
                log.info("Outbox: evento {} pubblicato su {}", event.getId(), event.getTopic());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                // Kafka non raggiungibile: l'evento resta in tabella e si riprova al prossimo giro.
                // Ci fermiamo al primo errore per non perdere l'ordine degli eventi.
                log.warn("Outbox: pubblicazione evento {} fallita, riprovo: {}", event.getId(), e.toString());
                return;
            }
        }
    }
}