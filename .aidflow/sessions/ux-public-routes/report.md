# ux-public-routes - Report

## Summary
JwtAuthFilter의 PUBLIC_PATHS에 `GET /api/v1/members/**` 경로를 추가하여 공개 프로필 API에 인증 없이 접근 가능하도록 했다.

## Plan Completion
- [x] 1: JwtAuthFilter PUBLIC_PATHS에 `new PathRule(HttpMethod.GET, "/api/v1/members/**")` 추가
- [x] 2: ./gradlew build 확인

## Changed Files
| File | Change Type | Description |
|------|-------------|-------------|
| src/main/java/com/eolma/gateway/filter/JwtAuthFilter.java | modified | PUBLIC_PATHS에 GET /api/v1/members/** 규칙 추가 |
| src/test/java/com/eolma/gateway/filter/JwtAuthFilterTest.java | modified | 인증 필요 테스트 2개의 HTTP 메서드를 GET에서 PUT으로 변경 (GET이 공개 경로가 되었으므로) |

## Key Decisions
- `GET /api/v1/members/**` 와일드카드 패턴을 사용하여 `/members/me`, `/members/{id}` 모두 공개로 설정
- `/members/me`의 인증 보호는 eolma-user의 SecurityConfig에서 이중으로 처리되므로 Gateway에서는 공개로 열어도 안전

## Issues & Observations
- 기존 테스트 2개(invalidAuthHeaderReturns401, expiredTokenReturns401)가 `GET /api/v1/members/me`를 사용하고 있었는데, 이 경로가 공개로 변경되면서 테스트가 깨질 수 있어 `PUT /api/v1/members/me`로 수정함
- eolma-user의 `ux-public-profile` 세션과 함께 배포해야 실제 동작 확인 가능

## Duration
- Started: 2026-03-05T10:21:29.928Z
- Completed: 2026-03-05
- Commits: 미커밋 (커밋 예정)
