package com.eolma.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * HTTP 요청/응답 로깅 필터.
 * traceId, method, path, status, duration을 구조화된 형태로 기록한다.
 */
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String START_TIME_ATTR = "requestStartTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getAttributes().put(START_TIME_ATTR, System.currentTimeMillis());

        ServerHttpRequest request = exchange.getRequest();
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        String method = request.getMethod().name();
        String path = request.getURI().getPath();

        log.debug("Incoming request: traceId={}, method={}, path={}", traceId, method, path);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Long startTime = exchange.getAttribute(START_TIME_ATTR);
            long duration = startTime != null ? System.currentTimeMillis() - startTime : -1;
            int statusCode = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value() : 0;

            log.info("Request completed: traceId={}, method={}, path={}, status={}, duration={}ms",
                    traceId, method, path, statusCode, duration);
        }));
    }

    @Override
    public int getOrder() {
        return -90; // TraceIdFilter 다음에 실행
    }
}
