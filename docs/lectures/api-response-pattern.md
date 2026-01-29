# API 응답 패턴과 예외 처리

## 왜 통일된 응답 형식이 필요한가?

```json
// ❌ 일관성 없음
GET /users/1      → { "id": 1, "name": "Kim" }
GET /habits       → [ {...}, {...} ]
POST /auth/login  → { "token": "..." }
에러 발생 시      → { "error": "Something went wrong" }

// ✅ 일관성 있음
{
  "success": true,
  "data": { ... },
  "error": null
}
```

---

## ApiResponse 패턴

### 1. 응답 래퍼 클래스

```java
@Getter
@Builder
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorResponse error;

    // 성공 응답
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .error(null)
                .build();
    }

    // 에러 응답
    public static ApiResponse<?> error(ErrorCode errorCode) {
        return ApiResponse.builder()
                .success(false)
                .data(null)
                .error(ErrorResponse.of(errorCode))
                .build();
    }
}
```

### 2. 에러 응답 클래스

```java
@Getter
@Builder
public class ErrorResponse {
    private final String code;
    private final String message;

    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }
}
```

### 3. 사용 예시

```java
@GetMapping("/habits")
public ResponseEntity<ApiResponse<List<HabitResponse>>> getHabits() {
    List<HabitResponse> habits = habitService.getHabits();
    return ResponseEntity.ok(ApiResponse.success(habits));
}
```

**응답:**
```json
{
  "success": true,
  "data": [
    { "id": 1, "name": "달리기" },
    { "id": 2, "name": "독서" }
  ],
  "error": null
}
```

---

## ErrorCode 패턴

### 에러 코드 정의

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_002", "서버 오류가 발생했습니다."),

    // Auth
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_001", "유효하지 않은 토큰입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_003", "인증이 필요합니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER_002", "이미 사용 중인 이메일입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
```

### 네이밍 규칙

```
도메인_설명
AUTH_001  → 인증 관련 1번 에러
USER_001  → 사용자 관련 1번 에러
HABIT_001 → 습관 관련 1번 에러
```

---

## BusinessException

### 비즈니스 예외 클래스

```java
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

### 사용 예시

```java
@Service
public class UserService {

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        // ...
    }
}
```

---

## GlobalExceptionHandler

### 전역 예외 처리

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 비즈니스 예외
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage());

        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode));
    }

    // Validation 예외 (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(
            MethodArgumentNotValidException e) {

        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE));
    }

    // 그 외 모든 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("Unexpected error: ", e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
```

---

## HTTP 상태 코드 가이드

| 상태 코드 | 의미 | 사용 상황 |
|----------|------|----------|
| 200 OK | 성공 | 조회, 수정 성공 |
| 201 Created | 생성됨 | 새 리소스 생성 |
| 204 No Content | 내용 없음 | 삭제 성공 |
| 400 Bad Request | 잘못된 요청 | 유효성 검증 실패 |
| 401 Unauthorized | 인증 필요 | 토큰 없음/만료 |
| 403 Forbidden | 권한 없음 | 본인 리소스 아님 |
| 404 Not Found | 없음 | 리소스 미존재 |
| 409 Conflict | 충돌 | 중복 데이터 |
| 500 Internal Server Error | 서버 오류 | 예기치 않은 에러 |

---

## 실전 흐름

```
1. 요청 수신
   POST /api/habits {name: "", type: "PRACTICE"}

2. Validation 실패
   @NotBlank 검증 → MethodArgumentNotValidException

3. GlobalExceptionHandler 처리
   → ApiResponse.error(INVALID_INPUT_VALUE)

4. 응답
   {
     "success": false,
     "data": null,
     "error": {
       "code": "COMMON_001",
       "message": "잘못된 입력값입니다."
     }
   }
```

---

## 장점

1. **프론트엔드 편의성**
   - 항상 같은 구조로 응답
   - success 필드로 간단히 성공/실패 판단

2. **유지보수성**
   - 에러 코드로 문제 추적 용이
   - 에러 메시지 한 곳에서 관리

3. **일관성**
   - 모든 API가 동일한 형식
   - 새로운 개발자도 쉽게 이해

---

**작성일:** 2025-01-29
