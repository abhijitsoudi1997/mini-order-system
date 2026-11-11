package com.abhijit.inventory_service.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

@SpringBootTest
@ActiveProfiles("test")
class TracingTest {

    @Autowired
    private Tracer tracer;

    @Test
    void aTracerIsConfigured() {
        assertThat(tracer).isNotNull();
    }

    @Test
    void aNewSpanHasIdsAndTheyReachTheLogContext() {
        Span span = tracer.nextSpan().name("test-span").start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            assertThat(span.context().traceId()).isNotBlank();
            assertThat(span.context().spanId()).isNotBlank();
            // this is why every log line can show the trace id
            assertThat(MDC.get("traceId")).isEqualTo(span.context().traceId());
        } finally {
            span.end();
        }
    }
}