# OAuth Gateway 공개 경로 추가

## Background
소셜 로그인 API 엔드포인트(login, link)는 인증 없이 접근 가능해야 한다. Gateway의 JwtAuthFilter에 공개 경로를 추가해야 한다.

## Objective
eolma-gateway의 JWT 인증 필터에 OAuth 관련 공개 경로 2개를 추가한다.

## Requirements

### Functional Requirements
- FR-1: POST /api/v1/auth/oauth/login 요청이 JWT 검증 없이 통과
- FR-2: POST /api/v1/auth/oauth/link 요청이 JWT 검증 없이 통과

## Out of Scope
- 인증이 필요한 OAuth 엔드포인트 (GET /accounts는 기존 인증 필터 통과)
- Rate Limiting 변경

## Technical Approach

기존 PUBLIC_PATHS 리스트에 PathRule 2개를 추가한다.

### Affected Files
- `src/main/java/com/eolma/gateway/filter/JwtAuthFilter.java` - PUBLIC_PATHS에 2개 항목 추가

## Implementation Items
- [x] JwtAuthFilter.java의 PUBLIC_PATHS에 추가:
  - `new PathRule(HttpMethod.POST, "/api/v1/auth/oauth/login")`
  - `new PathRule(HttpMethod.POST, "/api/v1/auth/oauth/link")`

## Acceptance Criteria
- [x] AC-1: OAuth login/link 요청이 401 없이 백엔드로 전달됨
- [x] AC-2: 기존 인증 경로 동작에 영향 없음

## Notes
- 매우 간단한 변경이므로 oauth-backend과 순차 진행 가능
