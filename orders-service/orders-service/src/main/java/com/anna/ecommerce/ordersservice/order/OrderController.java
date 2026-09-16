package com.anna.ecommerce.ordersservice.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order createOrder(@Valid @RequestBody CreateOrderRequest request) {
        List<OrderService.OrderItemRequest> items = request.items().stream()
                .map(i -> new OrderService.OrderItemRequest(i.productId(), i.quantity()))
                .toList();
        return orderService.createOrder(request.userId(), items);
    }

    @GetMapping("/{id}")
    public Order getOrder(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    public record CreateOrderRequest(
            @NotNull Long userId,
            @NotEmpty List<OrderItemDto> items
    ) {}

    public record OrderItemDto(
            @NotNull Long productId,
            @NotNull @Positive Integer quantity
    ) {}
}