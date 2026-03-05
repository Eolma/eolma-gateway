package com.eolma.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 검증 필터.
 * 공개 경로를 제외한 모든 요청에서 Authorization 헤더의 JWT를 검증한다.
 * 유효한 경우 X-User-Id, X-User-Email, X-User-Role 헤더를 주입하여 하위 서비스에 전달한다.
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey secretKey;

    // 인증이 필요 없는 공개 경로
    private static final List<PathRule> PUBLIC_PATHS = List.of(
            new PathRule(HttpMethod.POST, "/api/v1/auth/register"),
            new PathRule(HttpMethod.POST, "/api/v1/auth/login"),
            new PathRule(HttpMethod.POST, "/api/v1/auth/refresh"),
            new PathRule(HttpMethod.GET, "/api/v1/products/**"),
            new PathRule(HttpMethod.GET, "/api/v1/auctions/**"),
            new PathRule(HttpMethod.POST, "/api/v1/auth/oauth/login"),
            new PathRule(HttpMethod.POST, "/api/v1/auth/oauth/link"),
            new PathRule(HttpMethod.GET, "/api/v1/members/**")
    );

    // WebSocket 경로는 쿼리 파라미터로 토큰 검증 (별도 처리)
    private static final String WS_PATH_PREFIX = "/ws/";

    public JwtAuthFilter(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        // WebSocket 경로: token 쿼리 파라미터로 JWT 검증 후 X-User-Id 주입
        if (path.startsWith(WS_PATH_PREFIX)) {
            String tokenParam = request.getQueryParams().getFirst("token");
            if (tokenParam != null && !tokenParam.isBlank()) {
                try {
                    Claims wsClaims = Jwts.parser()
                            .verifyWith(secretKey)
                            .build()
                            .parseSignedClaims(tokenParam)
                            .getPayload();
                    ServerHttpRequest wsRequest = request.mutate()
                            .header("X-User-Id", wsClaims.getSubject())
                            .build();
                    return chain.filter(exchange.mutate().request(wsRequest).build());
                } catch (Exception ex) {
                    log.warn("WebSocket JWT validation failed: {}", ex.getMessage());
                }
            }
            return chain.filter(exchange);
        }

        // 공개 경로: JWT가 있으면 검증 시도하되, 실패해도 차단하지 않음
        if (isPublicPath(method, path)) {
            ServerWebExchange mutated = tryInjectUserHeaders(exchange);
            return chain.filter(mutated);
        }

        // 비공개 경로: JWT 필수
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            ServerHttpRequest mutatedRequest = injectHeaders(request, claims);
            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return unauthorized(exchange);
        }
    }

    private boolean isPublicPath(HttpMethod method, String path) {
        for (PathRule rule : PUBLIC_PATHS) {
            if (rule.method().equals(method) && matchPath(rule.pattern(), path)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchPath(String pattern, String path) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        return pattern.equals(path);
    }

    /**
     * Authorization 헤더가 있으면 JWT 검증 후 사용자 정보 헤더 주입을 시도한다.
     * 헤더가 없거나 검증 실패 시 원본 exchange를 그대로 반환한다.
     */
    private ServerWebExchange tryInjectUserHeaders(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return exchange;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            ServerHttpRequest mutatedRequest = injectHeaders(exchange.getRequest(), claims);
            return exchange.mutate().request(mutatedRequest).build();
        } catch (Exception e) {
            log.debug("Optional JWT validation failed on public path: {}", e.getMessage());
            return exchange;
        }
    }

    private ServerHttpRequest injectHeaders(ServerHttpRequest request, Claims claims) {
        String userId = claims.getSubject();
        String email = claims.get("email", String.class);
        String role = claims.get("role", String.class);

        return request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Email", email != null ? email : "")
                .header("X-User-Role", role != null ? role : "")
                .build();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -50; // TraceId, Logging 이후, 라우팅 이전
    }

    private record PathRule(HttpMethod method, String pattern) {}
}
