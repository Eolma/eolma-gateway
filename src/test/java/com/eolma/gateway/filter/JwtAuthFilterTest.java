package com.eolma.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthFilterTest {

    private static final String SECRET = "eolma-test-secret-key-minimum-256-bits-for-hmac-sha256-algorithm";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private JwtAuthFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(SECRET);
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    private String createToken(String subject, String email, String role, long expiryMs) {
        return Jwts.builder()
                .subject(subject)
                .claim("email", email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(KEY)
                .compact();
    }

    @Nested
    @DisplayName("공개 경로 테스트")
    class PublicPathTests {

        @Test
        @DisplayName("POST /api/v1/auth/login은 인증 없이 통과한다")
        void authLoginIsPublic() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .post("/api/v1/auth/login")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            filter.filter(exchange, chain).block();

            // 401이 아니면 통과
            assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("POST /api/v1/auth/register는 인증 없이 통과한다")
        void authRegisterIsPublic() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .post("/api/v1/auth/register")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            filter.filter(exchange, chain).block();

            assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("POST /api/v1/auth/refresh는 인증 없이 통과한다")
        void authRefreshIsPublic() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .post("/api/v1/auth/refresh")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            filter.filter(exchange, chain).block();

            assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("GET /api/v1/products는 인증 없이 통과한다")
        void getProductsIsPublic() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/v1/products")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            filter.filter(exchange, chain).block();

            assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("GET /api/v1/auctions/123은 인증 없이 통과한다")
        void getAuctionDetailIsPublic() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/v1/auctions/123")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            filter.filter(exchange, chain).block();

            assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("WebSocket 경로는 인증 없이 통과한다")
        void wsPathIsPublic() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/ws/auction/123")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            filter.filter(exchange, chain).block();

            assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("인증 필요 경로 테스트")
    class ProtectedPathTests {

        @Test
        @DisplayName("Authorization 헤더 없이 인증 필요 경로 접근 시 401")
        void noAuthHeaderReturns401() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .post("/api/v1/members/me")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            filter.filter(exchange, chain).block();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("잘못된 형식의 Authorization 헤더 시 401")
        void invalidAuthHeaderReturns401() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/v1/members/me")
                    .header(HttpHeaders.AUTHORIZATION, "InvalidToken")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            filter.filter(exchange, chain).block();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("만료된 JWT 시 401")
        void expiredTokenReturns401() {
            String expiredToken = Jwts.builder()
                    .subject("1")
                    .claim("email", "test@eolma.com")
                    .claim("role", "USER")
                    .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                    .expiration(new Date(System.currentTimeMillis() - 3600000))
                    .signWith(KEY)
                    .compact();

            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/v1/members/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            filter.filter(exchange, chain).block();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("유효한 JWT로 인증 시 X-User-Id 헤더가 주입된다")
        void validTokenInjectsUserHeaders() {
            String token = createToken("42", "user@eolma.com", "USER", 1800000);

            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/v1/members/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // chain.filter에서 mutated request의 헤더를 캡처
            when(chain.filter(any(ServerWebExchange.class))).thenAnswer(invocation -> {
                ServerWebExchange mutatedExchange = invocation.getArgument(0);
                assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("42");
                assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-User-Email")).isEqualTo("user@eolma.com");
                assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("USER");
                return Mono.empty();
            });

            filter.filter(exchange, chain).block();
        }

        @Test
        @DisplayName("POST /api/v1/products는 인증이 필요하다 (GET만 공개)")
        void postProductsRequiresAuth() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .post("/api/v1/products")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            filter.filter(exchange, chain).block();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }
}
