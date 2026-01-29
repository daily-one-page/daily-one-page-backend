# 05. UserHabit API (사용자 습관 등록/관리)

> 커밋: `feat: implement user-habit API for habit registration`

---

## 작업 내용

| API | Method | URL | 설명 |
|-----|--------|-----|------|
| 내 습관 목록 | GET | `/api/user-habits` | 등록한 습관 목록 조회 |
| 습관 등록 | POST | `/api/user-habits` | 습관 선택 및 등록 |
| 습관 해제 | DELETE | `/api/user-habits/{id}` | 습관 해제 (기록도 삭제) |

---

## 핵심 학습 포인트

### 1. Habit vs UserHabit 관계

```
┌─────────────────────────────────────────────────────────────┐
│                        Habit (습관 정의)                      │
│  id=1, name="달리기", type=PRACTICE, user_id=null (시스템)    │
│  id=2, name="독서", type=PRACTICE, user_id=null (시스템)      │
│  id=3, name="명상", type=PRACTICE, user_id=1 (커스텀)         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼ 사용자가 선택/등록
┌─────────────────────────────────────────────────────────────┐
│                   UserHabit (사용자별 등록)                    │
│  id=1, user_id=1, habit_id=1, streak=7, last_checked=01-28  │
│  id=2, user_id=1, habit_id=3, streak=3, last_checked=01-29  │
│  id=3, user_id=2, habit_id=1, streak=14, last_checked=01-29 │
└─────────────────────────────────────────────────────────────┘
```

**왜 분리했나?**
- 같은 "달리기" 습관이어도 사용자마다 스트릭, 마지막 체크일이 다름
- 한 사용자가 여러 습관을 등록 가능
- 한 습관을 여러 사용자가 등록 가능 (N:M 관계)

### 2. 커스텀 습관 등록 제한

```java
// 커스텀 습관인 경우, 본인 것만 등록 가능
if (habit.isCustomHabit() && !habit.isOwnedBy(userId)) {
    throw new BusinessException(ErrorCode.HABIT_NOT_OWNED);
}
```

**시나리오:**
- 시스템 습관 (user_id=null): 누구나 등록 가능
- 커스텀 습관 (user_id=1): user_id=1만 등록 가능

### 3. 중복 등록 방지

```java
// 중복 체크
if (userHabitRepository.existsByUserIdAndHabitId(userId, habitId)) {
    throw new BusinessException(ErrorCode.DUPLICATE_USER_HABIT);
}
```

**Repository 메서드:**
```java
boolean existsByUserIdAndHabitId(Long userId, Long habitId);
```

Spring Data JPA가 메서드명으로 쿼리 자동 생성:
```sql
SELECT EXISTS(
    SELECT 1 FROM user_habit
    WHERE user_id = ? AND habit_id = ?
)
```

### 4. JOIN FETCH로 N+1 문제 해결

```java
@Query("SELECT uh FROM UserHabit uh JOIN FETCH uh.habit WHERE uh.user.id = :userId")
List<UserHabit> findByUserIdWithHabit(@Param("userId") Long userId);
```

**N+1 문제란?**
```
// 일반 조회 시
1. SELECT * FROM user_habit WHERE user_id = 1  → 3건
2. SELECT * FROM habit WHERE id = 1  → habit 조회
3. SELECT * FROM habit WHERE id = 2  → habit 조회
4. SELECT * FROM habit WHERE id = 3  → habit 조회
= 총 4번 쿼리 (1 + N)
```

**JOIN FETCH 사용 시:**
```sql
SELECT uh.*, h.*
FROM user_habit uh
JOIN habit h ON uh.habit_id = h.id
WHERE uh.user_id = 1
-- 1번 쿼리로 해결!
```

### 5. 권한 검증 패턴

```java
public void unregisterHabit(Long userId, Long userHabitId) {
    UserHabit userHabit = userHabitRepository.findById(userHabitId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_HABIT_NOT_FOUND));

    // 본인 것만 해제 가능
    if (!userHabit.getUser().getId().equals(userId)) {
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    userHabitRepository.delete(userHabit);
}
```

**검증 순서:**
1. 리소스 존재 확인 (404)
2. 소유권 확인 (403)
3. 삭제 실행

---

## 파일 구조

```
domain/habit/
├── controller/
│   ├── HabitController.java
│   └── UserHabitController.java (추가)
├── service/
│   ├── HabitService.java
│   └── UserHabitService.java (추가)
├── dto/
│   ├── ... (기존 Habit DTOs)
│   ├── UserHabitCreateRequest.java (추가)
│   ├── UserHabitResponse.java (추가)
│   └── UserHabitListResponse.java (추가)
├── entity/
│   └── UserHabit.java (기존)
└── repository/
    └── UserHabitRepository.java (기존)
```

---

## API 테스트

### 1. 로그인 (토큰 발급)
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"password123"}'
```

### 2. 커스텀 습관 먼저 생성 (테스트용)
```bash
curl -X POST http://localhost:8080/api/habits \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{"name":"명상하기","type":"PRACTICE"}'

# 응답에서 habitId 확인 (예: 1)
```

### 3. 습관 등록
```bash
curl -X POST http://localhost:8080/api/user-habits \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{"habitId": 1}'
```

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "habitId": 1,
    "habitName": "명상하기",
    "habitType": "PRACTICE",
    "currentStreak": 0,
    "lastCheckedDate": null,
    "createdAt": "2025-01-29T10:00:00"
  }
}
```

### 4. 내 습관 목록 조회
```bash
curl http://localhost:8080/api/user-habits \
  -H "Authorization: Bearer {accessToken}"
```

### 5. 습관 해제
```bash
curl -X DELETE http://localhost:8080/api/user-habits/1 \
  -H "Authorization: Bearer {accessToken}"
```

---

## 검증 방법

1. 앱 실행
2. Swagger UI: `http://localhost:8080/swagger-ui/index.html`
3. UserHabit API 테스트
   - 커스텀 습관 생성 → 등록 → 목록 조회 → 해제
   - 중복 등록 시 409 에러 확인
   - 다른 사용자 습관 해제 시 403 에러 확인

---

## 성공 조건 (Acceptance Criteria)

이 커밋이 **성공**으로 간주되려면 아래 조건을 모두 만족해야 합니다.

### 빌드 성공
```bash
./gradlew compileJava
# BUILD SUCCESSFUL
```

### API 동작 확인

| # | 테스트 | 예상 결과 | 확인 |
|---|--------|----------|------|
| 1 | `POST /api/user-habits` (습관 등록) | 201 Created + UserHabitResponse 반환 | ☐ |
| 2 | `GET /api/user-habits` (목록 조회) | 200 OK + 등록한 습관 목록 | ☐ |
| 3 | `DELETE /api/user-habits/{id}` (해제) | 204 No Content | ☐ |
| 4 | 같은 습관 중복 등록 시도 | 409 Conflict (DUPLICATE_USER_HABIT) | ☐ |
| 5 | 존재하지 않는 습관 등록 시도 | 404 Not Found (HABIT_NOT_FOUND) | ☐ |
| 6 | 다른 사용자의 UserHabit 삭제 시도 | 403 Forbidden (ACCESS_DENIED) | ☐ |
| 7 | 토큰 없이 API 호출 | 401 Unauthorized | ☐ |

### 테스트 시나리오

```bash
# 0. 사전 준비: 회원가입 + 로그인
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"password123","nickname":"테스터"}'

curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"password123"}'
# → accessToken 저장

# 1. 커스텀 습관 생성
curl -X POST http://localhost:8080/api/habits \
  -H "Authorization: Bearer {TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"name":"명상하기","type":"PRACTICE"}'
# → habitId 확인 (예: 1)

# 2. 습관 등록 ✅
curl -X POST http://localhost:8080/api/user-habits \
  -H "Authorization: Bearer {TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"habitId": 1}'
# → 201 + userHabitId 확인

# 3. 중복 등록 시도 ❌
curl -X POST http://localhost:8080/api/user-habits \
  -H "Authorization: Bearer {TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"habitId": 1}'
# → 409 Conflict

# 4. 목록 조회 ✅
curl http://localhost:8080/api/user-habits \
  -H "Authorization: Bearer {TOKEN}"
# → 200 + 습관 목록

# 5. 습관 해제 ✅
curl -X DELETE http://localhost:8080/api/user-habits/1 \
  -H "Authorization: Bearer {TOKEN}"
# → 204 No Content

# 6. 다시 목록 조회 (비어있어야 함)
curl http://localhost:8080/api/user-habits \
  -H "Authorization: Bearer {TOKEN}"
# → 200 + habits: []
```

### 로컬 테스트 가능 여부

| 환경 | 가능 여부 | 비고 |
|------|----------|------|
| IntelliJ + H2 TCP | ✅ 가능 | H2 서버 먼저 실행 필요 |
| Swagger UI | ✅ 가능 | `http://localhost:8080/swagger-ui` |
| curl / Postman | ✅ 가능 | 위 시나리오 참고 |
| 이 환경 (Claude) | ❌ 불가 | 네트워크 제한 |

---

## 관련 학습 자료

- [JPA N+1 문제와 해결법](./lectures/jpa-n-plus-one.md)

---

**작성일:** 2025-01-29
