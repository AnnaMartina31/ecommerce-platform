package com.anna.ecommerce.ordersservice.order.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "event_key", nullable = false)
    private String eventKey;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(nullable = false)
    private Instant createdAt;

    // null = ancora da pubblicare
    private Instant publishedAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(String topic, String eventKey, String payload) {
        this.topic = topic;
        this.eventKey = eventKey;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTopic() { return topic; }
    public String getEventKey() { return eventKey; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }

    public void markPublished() {
        this.publishedAt = Instant.now();
    }
}