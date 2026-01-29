# 08. Badge API (뱃지 시스템)

> 커밋: `feat: implement badge API for achievement system`

---

## 작업 내용

| API | Method | URL | 설명 |
|-----|--------|-----|------|
| 내 뱃지 현황 | GET | `/api/badges/my` | 획득 + 진행 중 |
| 최근 획득 뱃지 | GET | `/api/badges/recent?limit=5` | 최근 N개 |
| 시스템 뱃지세트 | GET | `/api/badges/sets` | 범용 뱃지세트 목록 |
| 습관별 뱃지세트 | GET | `/api/badges/sets/habit/{id}` | 습관 전용 + 범용 |

---

## 핵심 학습 포인트

### 1. 뱃지 시스템 구조

```
┌─────────────────────────────────────────────────────────────┐
│                     BadgeSet (뱃지세트)                       │
│  id=1, name="스트릭 도전", habit_id=null (범용)              │
│  id=2, name="거리 도전", habit_id=1 (달리기 전용)            │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼ 1:N
┌─────────────────────────────────────────────────────────────┐
│                       Badge (개별 뱃지)                       │
│  id=1, badge_set_id=1, name="7일 연속", condition=7, seq=1  │
│  id=2, badge_set_id=1, name="30일 연속", condition=30, seq=2│
│  id=3, badge_set_id=1, name="100일 연속", condition=100,seq=3│
└─────────────────────────────────────────────────────────────┘
```

### 2. 범용 vs 전용 뱃지세트

```java
// 범용: 모든 습관에 적용 가능
habit_id = null, user_id = null  → isUniversal() = true

// 습관 전용: 특정 습관에만 적용
habit_id = 1, user_id = null     → isUniversal() = false

// 커스텀: 사용자가 만든 뱃지세트 (후순위 기능)
habit_id = ?, user_id = 1        → isSystem() = false
```

### 3. 진행률 계산

```java
public static UserBadgeSetResponse from(UserBadgeSet userBadgeSet) {
    int conditionValue = userBadgeSet.getCurrentBadge().getConditionValue();
    int currentValue = userBadgeSet.getCurrentValue();

    int remaining = Math.max(0, conditionValue - currentValue);
    int progress = (int) ((double) currentValue / conditionValue * 100);

    return UserBadgeSetResponse.builder()
            .currentValue(currentValue)      // 현재 5일
            .remainingValue(remaining)       // 남은 2일
            .progressPercent(Math.min(100, progress))  // 71%
            .build();
}
```

**예시:**
| 목표 | 현재 | 남은 값 | 진행률 |
|------|------|---------|--------|
| 7일 | 5일 | 2일 | 71% |
| 30일 | 30일 | 0일 | 100% |
| 100일 | 150일 | 0일 | 100% (초과해도 100%) |

### 4. 순차 뱃지 달성

```java
@OneToMany(mappedBy = "badgeSet", cascade = CascadeType.ALL)
@OrderBy("sequence ASC")  // 순서대로 정렬
private List<Badge> badges = new ArrayList<>();
```

**달성 흐름:**
```
스트릭 도전:
  [1] 7일 연속 → 달성! → UserBadge 생성
  [2] 30일 연속 → 현재 도전 중 (currentBadge)
  [3] 100일 연속 → 대기 중
```

### 5. JOIN FETCH 체이닝

```java
@Query("SELECT ub FROM UserBadge ub " +
       "JOIN FETCH ub.badge b " +           // 1단계: 뱃지
       "JOIN FETCH b.badgeSet " +            // 2단계: 뱃지세트
       "WHERE ub.user.id = :userId")
List<UserBadge> findByUserIdWithBadgeInfo(@Param("userId") Long userId);
```

**한 번의 쿼리로 3개 테이블 조인:**
```sql
SELECT ub.*, b.*, bs.*
FROM user_badge ub
JOIN badge b ON ub.badge_id = b.id
JOIN badge_set bs ON b.badge_set_id = bs.id
WHERE ub.user_id = ?
```

---

## 파일 구조

```
domain/badge/
├── controller/
│   └── BadgeController.java (추가)
├── service/
│   └── BadgeService.java (추가)
├── dto/
│   ├── BadgeResponse.java (추가)
│   ├── BadgeSetResponse.java (추가)
│   ├── UserBadgeResponse.java (추가)
│   ├── UserBadgeSetResponse.java (추가)
│   └── MyBadgesResponse.java (추가)
├── entity/
│   ├── Badge.java (기존)
│   ├── BadgeSet.java (기존)
│   ├── UserBadge.java (기존)
│   └── UserBadgeSet.java (기존)
└── repository/
    ├── BadgeRepository.java (기존)
    ├── BadgeSetRepository.java (기존)
    ├── UserBadgeRepository.java (기존)
    └── UserBadgeSetRepository.java (기존)
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
| 1 | `GET /api/badges/my` | 200 OK + 획득/진행 목록 | ☐ |
| 2 | `GET /api/badges/recent?limit=5` | 200 OK + 최근 뱃지 | ☐ |
| 3 | `GET /api/badges/sets` | 200 OK + 범용 뱃지세트 | ☐ |
| 4 | `GET /api/badges/sets/habit/1` | 200 OK + 전용+범용 | ☐ |
| 5 | 토큰 없이 호출 | 401 Unauthorized | ☐ |

### 테스트 시나리오

```bash
# 1. 내 뱃지 현황 조회
curl http://localhost:8080/api/badges/my \
  -H "Authorization: Bearer {TOKEN}"
# → 200 + acquired: [], inProgress: []

# 2. 시스템 뱃지세트 조회
curl http://localhost:8080/api/badges/sets \
  -H "Authorization: Bearer {TOKEN}"
# → 200 + 범용 뱃지세트 목록

# 3. 최근 획득 뱃지 (비어있을 수 있음)
curl "http://localhost:8080/api/badges/recent?limit=3" \
  -H "Authorization: Bearer {TOKEN}"
# → 200 + []
```

**참고:** 뱃지 획득 로직은 HabitLog 체크 시 자동 실행됩니다 (후속 작업).

---

## 관련 학습 자료

- [JPA N+1 문제와 해결법](./lectures/jpa-n-plus-one.md)

---

**작성일:** 2025-01-29
