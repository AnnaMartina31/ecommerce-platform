package com.anna.ecommerce.ordersservice.order.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductClientCircuitBreakerTest {

    private final ProductClient client =
            new ProductClient(org.springframework.web.client.RestClient.builder(), "http://localhost:1");

    @Test
    void whenServiceIsDown_circuitOpensAndLaterCallsFailFast() {
        // Le prime 5 chiamate falliscono per davvero (connessione rifiutata).
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> client.getProduct(1L))
                    .isInstanceOf(DependencyUnavailableException.class)
                    .hasMessageContaining("non raggiungibile");
        }

        // Il circuito ora è aperto: la chiamata successiva non tocca la rete.
        long start = System.nanoTime();
        assertThatThrownBy(() -> client.getProduct(1L))
                .isInstanceOf(DependencyUnavailableException.class)
                .hasMessageContaining("circuito aperto");
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isLessThan(50);
    }
}