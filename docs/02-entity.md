# 02. Entity & Repository

> 커밋: `feat: add JPA entities and repositories for all domains`

---

## 작업 내용

ERD 기반 10개 Entity + 9개 Repository 생성

| 도메인 | Entity | Repository |
|--------|--------|------------|
| User | User | UserRepository |
| Habit | Habit, UserHabit, HabitLog, HabitType(enum) | HabitRepository, UserHabitRepository, HabitLogRepository |
| Badge | BadgeSet, Badge, UserBadgeSet, UserBadge | BadgeSetRepository, BadgeRepository, UserBadgeSetRepository, UserBadgeRepository |
| DailyPage | DailyPage | DailyPageRepository |
| AI | AiFeedback | AiFeedbackRepository |

---

## 핵심 학습 포인트

### 1. JPA Entity 어노테이션

```java
@Entity                          // JPA 관리 엔티티
@Table(name = "users")           // 테이블명 (user는 예약어라 users로)
@Id                              // PK
@GeneratedValue(strategy = IDENTITY)  // Auto-increment
@Column(nullable = false)        // NOT NULL
@ManyToOne(fetch = LAZY)         // FK 관계
@JoinColumn(name = "user_id")    // FK 컬럼명
```

### 2. 왜 `@NoArgsConstructor(access = PROTECTED)`?

```java
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
```

- JPA는 프록시 생성 시 기본 생성자 필요 (Lazy Loading)
- `PROTECTED`로 외부에서 `new User()` 방지
- `@Builder`나 팩토리 메서드 사용 강제 → 유효한 객체만 생성

### 3. 왜 `fetch = FetchType.LAZY`?

```java
@ManyToOne(fetch = FetchType.LAZY)  // 기본값은 EAGER
private User user;
```

| FetchType | 동작 | 권장 |
|-----------|------|------|
| EAGER | 부모 조회 시 자식도 즉시 로딩 | ❌ N+1 문제 |
| LAZY | 실제 접근 시점에 로딩 | ✅ 기본 선택 |

### 4. N+1 문제와 해결

**문제:**
```java
List<UserHabit> habits = repository.findByUserId(1L);
for (UserHabit uh : habits) {
    uh.getHabit().getName();  // 매번 쿼리 발생!
}
```

**해결: JOIN FETCH**
```java
@Query("SELECT uh FROM UserHabit uh JOIN FETCH uh.habit WHERE uh.user.id = :userId")
List<UserHabit> findByUserIdWithHabit(@Param("userId") Long userId);
```
→ 한 번의 JOIN 쿼리로 해결

### 5. Unique Constraint (복합 유니크)

```java
@Table(name = "user_habit",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "habit_id"}))
```
→ 같은 사용자가 같은 습관 중복 추가 방지 (DB 레벨)

### 6. Enum 매핑

```java
@Enumerated(EnumType.STRING)  // "PRACTICE" 문자열로 저장
private HabitType type;
```

| EnumType | 저장값 | 주의 |
|----------|--------|------|
| ORDINAL | 0, 1, 2... | ❌ enum 순서 바뀌면 망함 |
| STRING | "PRACTICE" | ✅ 안전, 가독성 좋음 |

### 7. null FK 패턴 (시스템 vs 커스텀)

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")  // nullable
private User user;

// user_id = null → 시스템 습관 (모든 사용자 사용 가능)
// user_id = 123  → 커스텀 습관 (해당 사용자만)
```

### 8. Query Method vs @Query

**Query Method (단순 조회):**
```java
Optional<User> findByEmail(String email);
// → SELECT * FROM users WHERE email = ?

boolean existsByEmail(String email);
// → SELECT COUNT(*) > 0 FROM users WHERE email = ?
```

**@Query (복잡한 조회):**
```java
@Query("SELECT h FROM Habit h WHERE h.user IS NULL")
List<Habit> findSystemHabits();
```

---

## 검증 방법

1. **빌드 확인**: `Build → Build Project (Ctrl+F9)`
2. **앱 실행**: 에러 없이 시작되면 OK
3. **H2 Console 확인**: 테이블 10개 생성 확인
   - `jdbc:h2:tcp://localhost/~/dailyonepage/database/dailyonepage`

---

## 파일 구조

```
domain/
├── user/
│   ├── entity/User.java
│   └── repository/UserRepository.java
├── habit/
│   ├── entity/
│   │   ├── Habit.java
│   │   ├── HabitType.java (enum)
│   │   ├── UserHabit.java
│   │   └── HabitLog.java
│   └── repository/
│       ├── HabitRepository.java
│       ├── UserHabitRepository.java
│       └── HabitLogRepository.java
├── badge/
│   ├── entity/
│   │   ├── BadgeSet.java
│   │   ├── Badge.java
│   │   ├── UserBadgeSet.java
│   │   └── UserBadge.java
│   └── repository/
│       ├── BadgeSetRepository.java
│       ├── BadgeRepository.java
│       ├── UserBadgeSetRepository.java
│       └── UserBadgeRepository.java
├── dailypage/
│   ├── entity/DailyPage.java
│   └── repository/DailyPageRepository.java
└── ai/
    ├── entity/AiFeedback.java
    └── repository/AiFeedbackRepository.java
```

---

**작성일:** 2025-01-28
