package com.abhijit.api_gateway.filter;

import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String HEADER = "X-Correlation-Id";

    // Only plain, short ids are accepted from clients (keeps log lines clean)
    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9-]{1,64}$");

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        String correlationId = (incoming != null && VALID.matcher(incoming).matches())
                ? incoming
                : UUID.randomUUID().toString();

        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(HEADER, correlationId)
                .build();
        exchange.getResponse().getHeaders().set(HEADER, correlationId);

        String method = request.getMethod().name();
        String path = request.getURI().getRawPath();
        log.info("[{}] {} {}", correlationId, method, path);

        return chain.filter(exchange.mutate().request(request).build())
                .then(Mono.fromRunnable(() -> log.info("[{}] {} {} -> {}",
                        correlationId, method, path, exchange.getResponse().getStatusCode())));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;   // run first, so every other filter sees the id
    }
}