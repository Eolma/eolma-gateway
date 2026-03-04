package com.eolma.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TraceIdFilterTest {

    private TraceIdFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new TraceIdFilter();
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("X-Trace-Id가 없으면 새로 생성한다")
    void generatesTraceIdWhenMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(any(ServerWebExchange.class))).thenAnswer(invocation -> {
            ServerWebExchange mutated = invocation.getArgument(0);
            String traceId = mutated.getRequest().getHeaders().getFirst("X-Trace-Id");
            assertThat(traceId).isNotNull().isNotBlank();
            // UUID 형식 검증
            assertThat(traceId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
            return Mono.empty();
        });

        filter.filter(exchange, chain).block();
    }

    @Test
    @DisplayName("클라이언트가 보낸 X-Trace-Id를 그대로 사용한다")
    void preservesExistingTraceId() {
        String existingTraceId = "client-trace-id-123";
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .header("X-Trace-Id", existingTraceId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(any(ServerWebExchange.class))).thenAnswer(invocation -> {
            ServerWebExchange mutated = invocation.getArgument(0);
            String traceId = mutated.getRequest().getHeaders().getFirst("X-Trace-Id");
            assertThat(traceId).isEqualTo(existingTraceId);
            return Mono.empty();
        });

        filter.filter(exchange, chain).block();
    }

    @Test
    @DisplayName("응답에도 X-Trace-Id가 포함된다")
    void traceIdIncludedInResponse() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        String responseTraceId = exchange.getResponse().getHeaders().getFirst("X-Trace-Id");
        assertThat(responseTraceId).isNotNull().isNotBlank();
    }
}
