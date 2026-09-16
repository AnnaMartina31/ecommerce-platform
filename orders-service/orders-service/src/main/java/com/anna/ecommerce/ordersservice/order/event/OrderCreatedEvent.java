package com.anna.ecommerce.ordersservice.order.event;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        BigDecimal totalAmount,
        Instant createdAt
) {}