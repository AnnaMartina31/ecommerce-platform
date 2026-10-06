package com.anna.ecommerce.notificationsservice.notification;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        BigDecimal totalAmount,
        Instant createdAt
) {}