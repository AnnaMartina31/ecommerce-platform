package com.anna.ecommerce.ordersservice.order;

import com.anna.ecommerce.ordersservice.order.client.ProductClient;
import com.anna.ecommerce.ordersservice.order.client.UserClient;
import com.anna.ecommerce.ordersservice.order.event.OrderCreatedEvent;
import com.anna.ecommerce.ordersservice.order.event.OrderEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserClient userClient;
    private final ProductClient productClient;
    private final OrderEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    public OrderService(OrderRepository orderRepository, UserClient userClient,
                        ProductClient productClient, OrderEventPublisher eventPublisher,
                        TransactionTemplate transactionTemplate) {
        this.orderRepository = orderRepository;
        this.userClient = userClient;
        this.productClient = productClient;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
    }

    public record OrderItemRequest(Long productId, Integer quantity) {}

    public Order createOrder(Long userId, List<OrderItemRequest> itemRequests) {
        if (!userClient.userExists(userId)) {
            throw new IllegalArgumentException("Utente non trovato con id: " + userId);
        }

        List<OrderItem> items = itemRequests.stream()
                .map(this::resolveOrderItem)
                .collect(Collectors.toList());

        Order order = new Order(userId, items);

        return transactionTemplate.execute(status -> {
            Order savedOrder = orderRepository.save(order);
            eventPublisher.publishOrderCreated(new OrderCreatedEvent(
                    savedOrder.getId(),
                    savedOrder.getUserId(),
                    savedOrder.getTotalAmount(),
                    savedOrder.getCreatedAt()
            ));
            return savedOrder;
        });
    }

    private OrderItem resolveOrderItem(OrderItemRequest request) {
        ProductClient.ProductDto product = productClient.getProduct(request.productId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Prodotto non trovato con id: " + request.productId()));

        if (product.stockQuantity() < request.quantity()) {
            throw new IllegalArgumentException(
                    "Stock insufficiente per il prodotto: " + product.name());
        }

        return new OrderItem(product.id(), request.quantity(), product.price());
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}