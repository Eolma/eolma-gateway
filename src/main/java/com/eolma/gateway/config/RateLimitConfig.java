package com.eolma.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Rate Limiting 설정.
 * Redis 기반 RequestRateLimiter에서 사용할 KeyResolver를 정의한다.
 */
@Configuration
public class RateLimitConfig {

    /**
     * 클라이언트 IP 기반 Rate Limiting key resolver.
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just(ip);
        };
    }
}
