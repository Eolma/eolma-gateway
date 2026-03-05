# Gateway 공개 경로 추가

## Background

eolma-user에 공개 프로필 API(`GET /api/v1/members/{id}`)가 추가되면, Gateway의 JwtAuthFilter에서 해당 경로를 인증 면제(public) 경로로 등록해야 한다. 현재 `GET /api/v1/members/**`는 PUBLIC_PATHS에 없어서 JWT 인증이 강제된다.

## Objective

JwtAuthFilter의 PUBLIC_PATHS에 `GET /api/v1/members/**` 경로를 추가하여 공개 프로필 API에 인증 없이 접근 가능하도록 한다.

## Requirements

### Functional Requirements
- FR-1: `GET /api/v1/members/{id}` 요청이 JWT 없이 통과
- FR-2: `GET /api/v1/members/me`는 기존 동작 유지 (JWT 있으면 X-User-Id 주입, 없으면 통과만)
- FR-3: `PUT /api/v1/members/me`는 여전히 JWT 필수

### Non-Functional Requirements
- NFR-1: ./gradlew build 성공
- NFR-2: 기존 인증 흐름 변경 없음

## Out of Scope
- 라우팅 규칙 변경 (이미 `/api/v1/members/**` → user-service 라우트 존재)
- Rate Limiting 추가

## Technical Approach

`JwtAuthFilter.java`의 `PUBLIC_PATHS` 리스트에 한 줄 추가:
```java
new PathRule(HttpMethod.GET, "/api/v1/members/**")
```

현재 JwtAuthFilter 동작:
- PUBLIC_PATHS에 매칭되면 JWT 검증을 건너뜀
- 단, Authorization 헤더가 있으면 JWT를 파싱하여 X-User-Id를 주입 (optional auth)
- 이 동작은 `GET /api/v1/members/me`에서도 유용: 로그인 시 본인 프로필 조회, 비로그인 시에도 에러 없이 통과

### Affected Files

**수정**:
- `src/main/java/com/eolma/gateway/filter/JwtAuthFilter.java` - PUBLIC_PATHS에 GET /api/v1/members/** 추가

## Implementation Items
- [x] 1: JwtAuthFilter PUBLIC_PATHS에 `new PathRule(HttpMethod.GET, "/api/v1/members/**")` 추가
- [x] 2: ./gradlew build 확인

## Acceptance Criteria
- [ ] AC-1: GET /api/v1/members/1 요청이 JWT 없이 200 반환 (user-service까지 도달)
- [ ] AC-2: PUT /api/v1/members/me 요청은 JWT 없으면 401 반환
- [ ] AC-3: ./gradlew build 성공

## Notes
- `GET /api/v1/members/**`를 공개로 열면 `/api/v1/members/me`도 공개가 되지만, eolma-user의 SecurityConfig에서 `/me`는 인증을 요구하므로 이중 보호됨
- 라우팅은 기존 `user-service` 라우트(`/api/v1/members/**` → localhost:8081)가 이미 처리하므로 변경 불필요
- 이 작업은 eolma-user의 `ux-public-profile` 세션과 함께 배포해야 의미가 있음
