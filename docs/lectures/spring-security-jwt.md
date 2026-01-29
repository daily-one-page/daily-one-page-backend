# Spring Security + JWT 인증

## JWT란?

**JSON Web Token** - 사용자 인증 정보를 담은 토큰

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QHRlc3QuY29tIiwiaWF0IjoxNzA2...
     │                      │                                      │
   Header                 Payload                              Signature
  (알고리즘)             (사용자 정보)                          (서명)
```

---

## 세션 vs JWT

| 항목 | 세션 | JWT |
|------|------|-----|
| 저장 위치 | 서버 (메모리/DB) | 클라이언트 |
| 확장성 | 서버 증가 시 세션 공유 필요 | 서버 무관 (Stateless) |
| 서버 부하 | 높음 (세션 조회) | 낮음 (토큰 검증만) |
| 보안 | 서버가 세션 즉시 만료 가능 | 만료 전까지 유효 |

---

## 인증 흐름

```
1. 로그인 요청
   Client → POST /api/auth/login {email, password}

2. 토큰 발급
   Server → {accessToken, refreshToken}

3. API 요청 (토큰 포함)
   Client → GET /api/habits
            Authorization: Bearer eyJhbGci...

4. 토큰 검증
   JwtAuthenticationFilter → 토큰 파싱 → SecurityContext 저장

5. 응답
   Server → {habits: [...]}
```

---

## 핵심 컴포넌트

### 1. JwtTokenProvider

```java
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    // 토큰 생성
    public String createAccessToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .setSubject(email)           // 사용자 식별자
                .setIssuedAt(now)            // 발급 시간
                .setExpiration(expiry)       // 만료 시간
                .signWith(getSigningKey())   // 서명
                .compact();
    }

    // 토큰에서 이메일 추출
    public String getEmailFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
```

### 2. JwtAuthenticationFilter

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        // 1. 헤더에서 토큰 추출
        String token = resolveToken(request);

        // 2. 토큰 유효성 검증
        if (token != null && jwtTokenProvider.validateToken(token)) {
            // 3. 토큰에서 사용자 정보 추출
            String email = jwtTokenProvider.getEmailFromToken(token);

            // 4. 인증 객체 생성
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            // 5. SecurityContext에 저장
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        // 6. 다음 필터로 전달
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);  // "Bearer " 제거
        }
        return null;
    }
}
```

### 3. SecurityConfig

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF 비활성화 (JWT 사용 시)
                .csrf(csrf -> csrf.disable())

                // 세션 사용 안 함 (Stateless)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 경로별 인증 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()  // 인증 불필요
                        .anyRequest().authenticated())                // 나머지는 인증 필요

                // JWT 필터 추가
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)

                .build();
    }
}
```

---

## Access Token vs Refresh Token

| 항목 | Access Token | Refresh Token |
|------|-------------|---------------|
| 용도 | API 인증 | Access Token 재발급 |
| 유효 기간 | 짧음 (30분) | 길음 (7일) |
| 저장 위치 | 메모리/헤더 | HttpOnly Cookie |
| 탈취 시 위험 | 높음 (API 접근) | 중간 (재발급만) |

### 재발급 흐름

```
1. Access Token 만료
   API 요청 → 401 Unauthorized

2. Refresh Token으로 재발급
   POST /api/auth/reissue {refreshToken}

3. 새 토큰 발급
   Server → {newAccessToken, newRefreshToken}

4. 새 토큰으로 API 재요청
```

---

## 현재 사용자 조회

### SecurityUtil 사용

```java
public class SecurityUtil {
    public static String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return auth.getName();  // JWT의 subject (email)
    }
}
```

### Controller에서 사용

```java
@GetMapping("/my-habits")
public ResponseEntity<?> getMyHabits() {
    String email = SecurityUtil.getCurrentUserEmail();
    User user = userRepository.findByEmail(email).orElseThrow(...);
    // ...
}
```

---

## 보안 주의사항

1. **Secret Key**
   - 충분히 길게 (256bit 이상)
   - 환경 변수로 관리
   - 절대 코드에 하드코딩 ❌

2. **HTTPS 필수**
   - 토큰이 평문으로 전송되므로

3. **토큰 만료 시간**
   - Access: 짧게 (15분~1시간)
   - Refresh: 적당히 (1일~7일)

4. **로그아웃 처리**
   - 클라이언트: 토큰 삭제
   - 서버: Refresh Token 블랙리스트 (선택)

---

**작성일:** 2025-01-29
