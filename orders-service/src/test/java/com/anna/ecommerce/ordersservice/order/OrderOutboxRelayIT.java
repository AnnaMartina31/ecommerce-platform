package com.anna.ecommerce.ordersservice.order;

import com.anna.ecommerce.ordersservice.order.client.ProductClient;
import com.anna.ecommerce.ordersservice.order.client.UserClient;
import com.anna.ecommerce.ordersservice.order.outbox.OutboxEvent;
import com.anna.ecommerce.ordersservice.order.outbox.OutboxEventRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class OrderOutboxRelayIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @MockitoBean
    private UserClient userClient;

    @MockitoBean
    private ProductClient productClient;

    @Test
    void createOrder_withKafkaUp_relayPublishesEventToTopic() {
        when(userClient.userExists(1L)).thenReturn(true);
        when(productClient.getProduct(2L)).thenReturn(
                Optional.of(new ProductClient.ProductDto(2L, "Cuffie Bluetooth", new BigDecimal("89.90"), 10))
        );

        OrderController.CreateOrderRequest request = new OrderController.CreateOrderRequest(
                1L, List.of(new OrderController.OrderItemDto(2L, 2))
        );

        ResponseEntity<Order> response = restTemplate.postForEntity("/orders", request, Order.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long orderId = response.getBody().getId();

        // Il relay (ogni secondo) deve marcare l'evento come pubblicato.
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            List<OutboxEvent> events = outboxRepository.findAll().stream()
                    .filter(e -> e.getEventKey().equals(orderId.toString()))
                    .toList();
            assertThat(events).hasSize(1);
            assertThat(events.get(0).getPublishedAt()).isNotNull();
        });

        // Il messaggio deve essere davvero sul topic.
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "outbox-test",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        );
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of("order-events"));
            boolean found = false;
            long deadline = System.currentTimeMillis() + 15_000;
            while (!found && System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if (record.key().equals(orderId.toString())) {
                        assertThat(record.value()).contains("\"orderId\":" + orderId);
                        found = true;
                    }
                }
            }
            assertThat(found).as("messaggio con key %s sul topic order-events", orderId).isTrue();
        }
    }
}
