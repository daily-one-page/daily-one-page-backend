# 06. HabitLog API (습관 체크 기록)

> 커밋: `feat: implement habit-log API for daily check`

---

## 작업 내용

| API | Method | URL | 설명 |
|-----|--------|-----|------|
| 습관 체크 | POST | `/api/habit-logs` | 오늘(또는 지정일) 체크 |
| 기록 조회 | GET | `/api/habit-logs?date=2025-01-29` | 특정 날짜 기록 |
| 체크 취소 | DELETE | `/api/habit-logs/{id}` | 체크 삭제 + 스트릭 재계산 |

---

## 핵심 학습 포인트

### 1. 스트릭(Streak) 로직

```java
public void checkHabit(LocalDate today) {
    if (lastCheckedDate == null) {
        // 첫 체크
        this.currentStreak = 1;
    } else if (lastCheckedDate.equals(today.minusDays(1))) {
        // 연속 체크 (어제 체크했으면)
        this.currentStreak += 1;
    } else if (!lastCheckedDate.equals(today)) {
        // 연속 끊김 (어제가 아니면 리셋)
        this.currentStreak = 1;
    }
    this.lastCheckedDate = today;
}
```

**스트릭 시나리오:**

| 상황 | lastCheckedDate | today | 결과 |
|------|----------------|-------|------|
| 첫 체크 | null | 01-29 | streak = 1 |
| 연속 | 01-28 | 01-29 | streak += 1 |
| 끊김 | 01-25 | 01-29 | streak = 1 (리셋) |
| 중복 | 01-29 | 01-29 | 변화 없음 |

### 2. 체크 취소 시 스트릭 재계산

```java
private void recalculateStreak(UserHabit userHabit) {
    List<HabitLog> checkedLogs = habitLogRepository
            .findCheckedLogsByUserHabitIdOrderByDateDesc(userHabit.getId());

    if (checkedLogs.isEmpty()) {
        userHabit.resetStreak();
        return;
    }

    // 연속된 날짜 계산
    int streak = 0;
    LocalDate expectedDate = LocalDate.now();

    for (HabitLog log : checkedLogs) {
        if (log.getDate().equals(expectedDate) ||
            log.getDate().equals(expectedDate.minusDays(1))) {
            streak++;
            expectedDate = log.getDate().minusDays(1);
        } else {
            break;  // 연속 끊김
        }
    }

    userHabit.recalculateStreak(streak, lastDate);
}
```

**왜 재계산이 필요한가?**
- 중간 날짜 체크 취소 시 스트릭이 끊길 수 있음
- 단순히 -1 하면 부정확

### 3. 날짜 파라미터 처리

```java
@GetMapping
public ResponseEntity<...> getLogsByDate(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

    LocalDate targetDate = date != null ? date : LocalDate.now();
    // ...
}
```

**`@DateTimeFormat` 어노테이션:**
- 문자열 → LocalDate 자동 변환
- `ISO.DATE` = "2025-01-29" 형식

### 4. 습관 타입별 체크 의미

```java
public enum HabitType {
    PRACTICE,    // 실천형: checked=true → 성공
    ABSTINENCE   // 절제형: checked=true → 실패 (유혹에 넘어감)
}
```

| 타입 | checked=true 의미 | checked=false 의미 |
|------|------------------|-------------------|
| PRACTICE | 오늘 했다 ✅ | 안 했다 ❌ |
| ABSTINENCE | 유혹에 넘어감 ❌ | 잘 참음 ✅ |

**UI 표시 예시:**
- 달리기(PRACTICE): 체크 = 🏃 했음
- 금연(ABSTINENCE): 체크 = 🚬 피움 (실패)

### 5. 유니크 제약조건

```java
@Table(uniqueConstraints =
    @UniqueConstraint(columnNames = {"user_habit_id", "date"}))
public class HabitLog {
```

**의미:** 같은 습관 + 같은 날짜에 중복 기록 방지

```sql
-- DB 레벨에서 중복 방지
UNIQUE INDEX idx_user_habit_date (user_habit_id, date)
```

### 6. 트랜잭션과 일관성

```java
@Transactional
public HabitLogResponse checkHabit(Long userId, HabitLogCreateRequest request) {
    // 1. UserHabit 조회
    // 2. HabitLog 생성 + 저장
    // 3. 스트릭 업데이트 (같은 트랜잭션)

    // 모두 성공 or 모두 롤백
}
```

**왜 중요한가?**
- HabitLog만 저장되고 스트릭 업데이트 실패 → 데이터 불일치
- 트랜잭션으로 원자성 보장

---

## 파일 구조

```
domain/habit/
├── controller/
│   └── HabitLogController.java (추가)
├── service/
│   └── HabitLogService.java (추가)
├── dto/
│   ├── HabitLogCreateRequest.java (추가)
│   ├── HabitLogResponse.java (추가)
│   └── HabitLogListResponse.java (추가)
├── entity/
│   └── HabitLog.java (기존)
└── repository/
    └── HabitLogRepository.java (기존)

global/exception/
└── ErrorCode.java (에러 코드 추가)
```

---

## 성공 조건 (Acceptance Criteria)

### 빌드 성공
```bash
./gradlew compileJava
# BUILD SUCCESSFUL
```

### API 동작 확인

| # | 테스트 | 예상 결과 | 확인 |
|---|--------|----------|------|
| 1 | `POST /api/habit-logs` | 201 Created + streak 업데이트 | ☐ |
| 2 | `GET /api/habit-logs` (오늘) | 200 OK + 오늘 체크 목록 | ☐ |
| 3 | `GET /api/habit-logs?date=...` | 200 OK + 해당 날짜 목록 | ☐ |
| 4 | `DELETE /api/habit-logs/{id}` | 204 No Content + 스트릭 재계산 | ☐ |
| 5 | 같은 날짜 중복 체크 | 409 Conflict | ☐ |
| 6 | 존재하지 않는 UserHabit | 404 Not Found | ☐ |
| 7 | 다른 사용자 습관 체크 | 403 Forbidden | ☐ |

### 테스트 시나리오

```bash
# 0. 사전 준비: 로그인 + 습관 등록
# (이전 단계 참고)

# 1. 습관 체크 ✅
curl -X POST http://localhost:8080/api/habit-logs \
  -H "Authorization: Bearer {TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"userHabitId": 1}'
# → 201 + currentStreak: 1

# 2. 중복 체크 시도 ❌
curl -X POST http://localhost:8080/api/habit-logs \
  -H "Authorization: Bearer {TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"userHabitId": 1}'
# → 409 Conflict

# 3. 오늘 기록 조회 ✅
curl http://localhost:8080/api/habit-logs \
  -H "Authorization: Bearer {TOKEN}"
# → 200 + logs: [...]

# 4. 체크 취소 ✅
curl -X DELETE http://localhost:8080/api/habit-logs/1 \
  -H "Authorization: Bearer {TOKEN}"
# → 204 No Content

# 5. 다시 조회 (비어있어야 함)
curl http://localhost:8080/api/habit-logs \
  -H "Authorization: Bearer {TOKEN}"
# → 200 + logs: []
```

---

## 관련 학습 자료

- [JPA N+1 문제와 해결법](./lectures/jpa-n-plus-one.md)
- [Spring에서 날짜/시간 다루기](./lectures/spring-datetime.md)

---

**작성일:** 2025-01-29
