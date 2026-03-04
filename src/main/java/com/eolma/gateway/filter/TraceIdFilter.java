package com.eolma.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 모든 요청에 X-Trace-Id 헤더를 생성하여 하위 서비스로 전파한다.
 * 클라이언트가 이미 X-Trace-Id를 보낸 경우 그대로 사용한다.
 */
@Component
public class TraceIdFilter implements GlobalFilter, Ordered {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(TRACE_ID_HEADER, traceId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        // 응답에도 traceId 포함
        mutatedExchange.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId);

        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        return -100; // 가장 먼저 실행
    }
}
