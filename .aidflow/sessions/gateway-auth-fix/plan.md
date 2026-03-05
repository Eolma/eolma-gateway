# Gateway 인증 필터 수정 - QA #2, #3

## 문제 요약

### #2 [HIGH] 내 입찰 API 500 에러 (`GET /api/v1/auctions/me`)
### #3 [HIGH] 내 상품 API X-User-Id 누락 (`GET /api/v1/products/me`)

**근본 원인**: `JwtAuthFilter`의 PUBLIC_PATHS에 와일드카드 패턴이 등록되어 있음:
- `GET /api/v1/auctions/**` → `/api/v1/auctions/me`까지 매칭
- `GET /api/v1/products/**` → `/api/v1/products/me`까지 매칭

public path로 판단되면 JWT 검증을 완전히 건너뛰어 X-User-Id 헤더가 주입되지 않음.
백엔드 컨트롤러에서 `@RequestHeader("X-User-Id")` 필수 파라미터가 없어 500/400 에러 발생.

## 해결 방안

public path 요청에서도 Authorization 헤더가 존재하면 JWT를 검증하고 사용자 정보 헤더를 주입 (optional auth).
JWT가 없거나 만료되어도 요청을 차단하지 않고 통과시킴.

## 구현 계획

### 수정 파일
- `src/main/java/com/eolma/gateway/filter/JwtAuthFilter.java`

### 변경 내용
- [x] `filter()` 메서드에서 public path일 때 바로 `chain.filter(exchange)`를 호출하는 대신, Authorization 헤더가 있으면 JWT 검증을 시도하고 성공 시 헤더 주입 후 통과, 실패 시에도 그냥 통과 (인증 실패로 차단하지 않음)
- [x] JWT 검증 + 헤더 주입 로직을 별도 private 메서드로 추출하여 재사용

### 검증
- [ ] 인증 없이 `GET /api/v1/auctions` 목록 조회 → 정상 동작 확인
- [ ] 인증 있이 `GET /api/v1/auctions/me` → X-User-Id 주입되어 정상 응답
- [ ] 인증 없이 `GET /api/v1/products/1` → 정상 동작 확인
- [ ] 인증 있이 `GET /api/v1/products/me` → X-User-Id 주입되어 정상 응답
