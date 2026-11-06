package com.abhijit.api_gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import reactor.core.publisher.Mono;

@SpringBootTest(properties = {"eureka.client.enabled=false", "spring.cloud.discovery.enabled=false"})
class RoutesTest {

    @Autowired
    private RouteLocator routeLocator;

    private List<Route> routes() {
        return routeLocator.getRoutes().collectList().block();
    }

    private Route route(String id) {
        return routes().stream().filter(r -> r.getId().equals(id)).findFirst().orElseThrow();
    }

    private boolean matches(Route route, String path) {
        MockServerWebExchange ex = MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
        return Boolean.TRUE.equals(Mono.from(route.getPredicate().apply(ex)).block());
    }

    @Test
    void exactlyTwoRoutesAreDefined() {
        assertThat(routes()).hasSize(2);
    }

    @Test
    void productsGoToInventoryByServiceName() {
        Route r = route("inventory-products");
        assertThat(r.getUri().toString()).isEqualTo("lb://inventory-service");
        assertThat(matches(r, "/api/products/sku/SKU-1001")).isTrue();
        assertThat(matches(r, "/api/products")).isTrue();
    }

    @Test
    void ordersGoToOrderServiceByServiceName() {
        Route r = route("order-orders");
        assertThat(r.getUri().toString()).isEqualTo("lb://order-service");
        assertThat(matches(r, "/api/orders/5/cancel")).isTrue();
    }

    @Test
    void internalAndUnprefixedPathsMatchNoRoute() {
        for (String path : List.of("/api/inventory/reserve", "/api/inventory/release",
                "/products", "/orders", "/actuator/env")) {
            assertThat(routes().stream().anyMatch(r -> matches(r, path)))
                    .as("some route matched %s", path).isFalse();
        }
    }
}