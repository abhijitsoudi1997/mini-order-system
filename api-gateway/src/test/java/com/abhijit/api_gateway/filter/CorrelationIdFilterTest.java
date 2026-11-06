package com.abhijit.api_gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    private final AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
    private final GatewayFilterChain chain = ex -> {
        forwarded.set(ex);
        return Mono.empty();
    };

    private MockServerWebExchange exchange(String idHeader) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.get("/api/orders");
        if (idHeader != null) {
            builder.header(CorrelationIdFilter.HEADER, idHeader);
        }
        return MockServerWebExchange.from(builder.build());
    }

    @Test
    void generatesAnIdWhenTheClientSendsNone() {
        MockServerWebExchange ex = exchange(null);
        filter.filter(ex, chain).block();

        String forwardedId = forwarded.get().getRequest().getHeaders().getFirst(CorrelationIdFilter.HEADER);
        assertThat(forwardedId).isNotBlank();
        assertThat(ex.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER)).isEqualTo(forwardedId);
    }

    @Test
    void keepsAValidIdFromTheClient() {
        MockServerWebExchange ex = exchange("abc-123");
        filter.filter(ex, chain).block();

        assertThat(forwarded.get().getRequest().getHeaders().getFirst(CorrelationIdFilter.HEADER))
                .isEqualTo("abc-123");
        assertThat(ex.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER)).isEqualTo("abc-123");
    }

    @Test
    void replacesAnInvalidId() {
        MockServerWebExchange ex = exchange("bad id with spaces & symbols");
        filter.filter(ex, chain).block();

        String forwardedId = forwarded.get().getRequest().getHeaders().getFirst(CorrelationIdFilter.HEADER);
        assertThat(forwardedId).isNotEqualTo("bad id with spaces & symbols");
        assertThat(forwardedId).matches("^[A-Za-z0-9-]{1,64}$");
    }

    @Test
    void replacesAnIdThatIsTooLong() {
        MockServerWebExchange ex = exchange("a".repeat(65));
        filter.filter(ex, chain).block();

        assertThat(forwarded.get().getRequest().getHeaders().getFirst(CorrelationIdFilter.HEADER))
                .hasSizeLessThanOrEqualTo(64);
    }

    @Test
    void runsFirst() {
        assertThat(filter.getOrder()).isEqualTo(org.springframework.core.Ordered.HIGHEST_PRECEDENCE);
    }
}