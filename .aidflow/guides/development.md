# eolma-gateway Development Guide

## 서비스 개요

API Gateway. 모든 외부 요청의 진입점으로, JWT 인증, Rate Limiting, 요청 추적을 담당한다.

- 포트: 8080
- 프레임워크: Spring Cloud Gateway (WebFlux 기반)
- 캐시: Redis (Rate Limiting)

## 필터 체인

실행 순서 (order 값이 낮을수록 먼저 실행):

| 순서 | 필터 | Order | 역할 |
|------|------|-------|------|
| 1 | TraceIdFilter | -100 | X-Trace-Id 생성/전파 |
| 2 | RequestLoggingFilter | -90 | 요청/응답 로깅 (method, path, status, duration) |
| 3 | JwtAuthFilter | -50 | JWT 검증, 사용자 정보 헤더 주입 |

## JWT 인증 필터

### 동작 방식
1. `Authorization: Bearer {token}` 헤더에서 JWT 추출
2. JWT 검증 (서명, 만료 확인)
3. 유효하면 하위 서비스로 전달할 헤더 주입:
   - `X-User-Id`: 회원 ID
   - `X-User-Email`: 이메일
   - `X-User-Role`: 역할 (USER/ADMIN)
4. 무효하면 401 Unauthorized 응답

### 공개 경로 (인증 제외)
- `POST /api/v1/auth/**` (회원가입, 로그인, 토큰 갱신)
- `GET /api/v1/products/**` (상품 조회)
- `GET /api/v1/auctions/**` (경매 조회)

## 라우팅 규칙

| Route ID | 경로 패턴 | 대상 서비스 | Rate Limit |
|----------|----------|------------|------------|
| auth-service | `POST /api/v1/auth/**` | localhost:8081 | 5 req/s |
| user-service | `/api/v1/members/**` | localhost:8081 | - |
| product-service | `/api/v1/products/**` | localhost:8082 | - |
| auction-service-rest | `/api/v1/auctions/**` | localhost:8083 | 20 req/s |
| auction-service-ws | `/ws/auction/**` | ws://localhost:8083 | - |
| payment-service | `/api/v1/payments/**` | localhost:8084 | 5 req/s |

## Rate Limiting

Redis 기반 RequestRateLimiter 사용:
- Key Resolver: 클라이언트 IP 기반
- 설정값은 라우트별로 `application.yml`에서 관리
- `replenishRate`: 초당 허용 요청 수
- `burstCapacity`: 최대 버스트 허용 수

## CORS 설정

`application.yml`의 `spring.cloud.gateway.globalcors`에서 관리:
- Allowed origins: `http://localhost:3000` (개발 환경)
- Allowed methods: GET, POST, PUT, DELETE, OPTIONS
- Allowed headers: Authorization, Content-Type 등

## Trace ID

모든 요청에 `X-Trace-Id`를 부여하여 서비스 간 분산 추적을 지원:
- 클라이언트가 보낸 X-Trace-Id가 있으면 그대로 사용
- 없으면 UUID를 생성하여 주입
- 하위 서비스에서 로그에 traceId를 포함하여 요청 추적 가능

## 주의사항

- Gateway는 외부 클라이언트(프론트엔드) 전용. 서비스 간 내부 통신에 사용 금지
- JWT Secret은 eolma-user 서비스와 동일한 값을 사용해야 함
- WebSocket 라우트(`/ws/auction/**`)는 별도 설정 (ws:// 프로토콜)
- Gateway 자체에는 DB가 없음
