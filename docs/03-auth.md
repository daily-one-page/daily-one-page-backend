# 03. Auth API (인증)

> 커밋: `feat: implement auth API with JWT authentication`

---

## 작업 내용

| API | Method | URL | 설명 |
|-----|--------|-----|------|
| 회원가입 | POST | `/api/auth/signup` | 새 사용자 등록 |
| 로그인 | POST | `/api/auth/login` | Access Token + Refresh Token 발급 |
| 토큰 재발급 | POST | `/api/auth/reissue` | Refresh Token으로 새 토큰 발급 |

---

## 핵심 학습 포인트

### 1. JWT (JSON Web Token) 구조

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGVtYWlsLmNvbSIsImlhdCI6MTcwNjQ1...
├── Header ───────────┤├── Payload ────────────────────────────────────┤├── Signature ─┤
```

| 부분 | 내용 |
|------|------|
| **Header** | 알고리즘 (HS256), 토큰 타입 (JWT) |
| **Payload** | 사용자 정보 (sub: email), 발급시간 (iat), 만료시간 (exp) |
| **Signature** | Header + Payload + Secret Key로 생성한 서명 |

### 2. Access Token vs Refresh Token

| 구분 | Access Token | Refresh Token |
|------|--------------|---------------|
| **용도** | API 요청 인증 | Access Token 재발급 |
| **만료 시간** | 30분 (짧게) | 7일 (길게) |
| **저장 위치** | 메모리 / localStorage | httpOnly Cookie (권장) |
| **탈취 시 위험** | 30분간 악용 가능 | 새 토큰 발급 가능 (더 위험) |

**왜 두 개로 나누나?**
- Access Token만 쓰면: 만료 짧으면 → 자주 로그인, 길면 → 탈취 위험
- 분리하면: Access Token 짧게 + Refresh Token으로 갱신 → 보안 + 편의성

### 3. JWT 인증 흐름

```
[로그인]
Client → POST /api/auth/login (email, password)
Server → 검증 후 Access Token + Refresh Token 발급
Client ← 토큰 저장

[API 요청]
Client → GET /api/habits (Header: Authorization: Bearer {accessToken})
Server → JwtAuthenticationFilter에서 토큰 검증
Server → SecurityContext에 인증 정보 저장
Server → Controller 실행
Client ← 응답

[토큰 만료 시]
Client → API 요청 → 401 Unauthorized
Client → POST /api/auth/reissue (refreshToken)
Server → 새 Access Token + Refresh Token 발급
Client ← 토큰 갱신 후 재요청
```

### 4. Spring Security Filter Chain

```
요청 → [JwtAuthenticationFilter] → [UsernamePasswordAuthenticationFilter] → ... → Controller
        ↓
        토큰 검증 → Authentication 객체 생성 → SecurityContextHolder에 저장
```

**OncePerRequestFilter를 쓰는 이유:**
- 일반 Filter는 forward/include 시 여러 번 실행될 수 있음
- OncePerRequestFilter는 요청당 **딱 한 번만** 실행 보장

### 5. @ConfigurationProperties

```java
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private long accessTokenExpiration;
}
```

```yaml
jwt:
  secret: my-secret-key
  access-token-expiration: 1800000
```

- `@Value`보다 타입 안전하고 관리 용이
- 관련 설정을 객체로 묶어서 관리

### 6. PasswordEncoder (BCrypt)

```java
// 암호화
String encoded = passwordEncoder.encode("password123");
// → $2a$10$N9qo8uLOickgx2ZMRZoMy...

// 검증
boolean matches = passwordEncoder.matches("password123", encoded);
// → true
```

**BCrypt 특징:**
- 단방향 해시 (복호화 불가)
- Salt 자동 생성 (같은 비밀번호도 다른 해시값)
- Cost Factor로 연산 속도 조절 (브루트포스 방어)

### 7. @Transactional(readOnly = true)

```java
@Service
@Transactional(readOnly = true)  // 기본: 읽기 전용
public class AuthService {

    @Transactional  // 쓰기가 필요한 메서드만 오버라이드
    public Long signup(SignupRequest request) { ... }

    // readOnly 상속
    public TokenResponse login(LoginRequest request) { ... }
}
```

**readOnly = true 효과:**
- JPA 더티체킹 비활성화 → 성능 향상
- 실수로 데이터 변경 방지

### 8. Validation 어노테이션

```java
public class SignupRequest {
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
    private String password;
}
```

| 어노테이션 | 검증 내용 |
|-----------|----------|
| `@NotBlank` | null, "", " " 모두 불가 |
| `@NotNull` | null만 불가 |
| `@NotEmpty` | null, "" 불가 (공백은 허용) |
| `@Email` | 이메일 형식 검증 |
| `@Size` | 길이 제한 |
| `@Pattern` | 정규식 검증 |

### 9. SecurityContextHolder

```java
// 현재 인증된 사용자 정보 가져오기
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String email = auth.getName();

// 또는 Controller에서
@AuthenticationPrincipal UserDetails userDetails
```

**SecurityContext 저장 전략:**
- `MODE_THREADLOCAL` (기본): 같은 스레드 내에서 공유
- `MODE_INHERITABLETHREADLOCAL`: 자식 스레드에도 전파
- `MODE_GLOBAL`: 전역 (잘 안 씀)

---

## 파일 구조

```
domain/auth/
├── controller/
│   └── AuthController.java
├── service/
│   └── AuthService.java
└── dto/
    ├── SignupRequest.java
    ├── LoginRequest.java
    ├── TokenResponse.java
    └── TokenReissueRequest.java

global/security/
├── jwt/
│   ├── JwtProperties.java
│   ├── JwtTokenProvider.java
│   └── JwtAuthenticationFilter.java
└── CustomUserDetailsService.java
```

---

## API 테스트

### 회원가입
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"password123","nickname":"테스터"}'
```

### 로그인
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"password123"}'
```

### 인증이 필요한 API 호출
```bash
curl http://localhost:8080/api/some-endpoint \
  -H "Authorization: Bearer {accessToken}"
```

---

## 검증 방법

1. 앱 실행
2. Swagger UI: `http://localhost:8080/swagger-ui/index.html`
3. Auth API 테스트
   - 회원가입 → 로그인 → 토큰 확인
   - 토큰으로 인증 필요한 API 호출 테스트

---

**작성일:** 2025-01-28
