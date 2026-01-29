# JPA N+1 문제와 해결법

## N+1 문제란?

연관 관계가 있는 엔티티를 조회할 때, 1번의 쿼리로 N개의 데이터를 가져온 후 각 데이터마다 추가 쿼리가 N번 발생하는 문제.

---

## 예시 상황

```java
@Entity
public class UserHabit {
    @ManyToOne(fetch = FetchType.LAZY)
    private Habit habit;  // 연관 엔티티
}
```

### 문제 발생 코드

```java
List<UserHabit> userHabits = userHabitRepository.findByUserId(1L);  // 1번 쿼리

for (UserHabit uh : userHabits) {
    System.out.println(uh.getHabit().getName());  // 매번 쿼리 발생!
}
```

### 실제 발생하는 쿼리

```sql
-- 1번: UserHabit 조회
SELECT * FROM user_habit WHERE user_id = 1;
-- 결과: 3건

-- N번: 각 UserHabit의 Habit 조회
SELECT * FROM habit WHERE id = 1;
SELECT * FROM habit WHERE id = 2;
SELECT * FROM habit WHERE id = 3;

-- 총 4번 쿼리 (1 + 3)
```

**데이터가 100개면?** → 101번 쿼리 발생!

---

## 해결 방법

### 1. JOIN FETCH (JPQL)

```java
@Query("SELECT uh FROM UserHabit uh JOIN FETCH uh.habit WHERE uh.user.id = :userId")
List<UserHabit> findByUserIdWithHabit(@Param("userId") Long userId);
```

**발생 쿼리:**
```sql
SELECT uh.*, h.*
FROM user_habit uh
JOIN habit h ON uh.habit_id = h.id
WHERE uh.user_id = 1;
-- 1번 쿼리로 끝!
```

### 2. @EntityGraph (Spring Data JPA)

```java
@EntityGraph(attributePaths = {"habit"})
List<UserHabit> findByUserId(Long userId);
```

### 3. @BatchSize (Hibernate)

```java
@Entity
public class Habit {
    @BatchSize(size = 100)  // 한 번에 100개씩 IN 쿼리
    @OneToMany(mappedBy = "habit")
    private List<UserHabit> userHabits;
}
```

**발생 쿼리:**
```sql
SELECT * FROM habit WHERE id IN (1, 2, 3, ...);  -- 묶어서 조회
```

---

## 방법 비교

| 방법 | 장점 | 단점 |
|------|------|------|
| JOIN FETCH | 1번 쿼리, 정확한 제어 | JPQL 작성 필요, 페이징 주의 |
| @EntityGraph | 메서드명으로 간편 | 복잡한 조건 어려움 |
| @BatchSize | 전역 설정 가능 | IN 쿼리 여러 번 가능 |

---

## 주의사항

### 1. LAZY vs EAGER

```java
// ❌ EAGER: 항상 함께 조회 (N+1 발생 가능)
@ManyToOne(fetch = FetchType.EAGER)
private Habit habit;

// ✅ LAZY: 필요할 때만 조회 (기본 권장)
@ManyToOne(fetch = FetchType.LAZY)
private Habit habit;
```

### 2. 컬렉션 JOIN FETCH와 페이징

```java
// ⚠️ 컬렉션 JOIN FETCH + 페이징 = 메모리에서 페이징 (위험!)
@Query("SELECT u FROM User u JOIN FETCH u.habits")
Page<User> findAllWithHabits(Pageable pageable);  // 경고 발생
```

**해결:** `@BatchSize` 사용 또는 쿼리 분리

### 3. 둘 이상 컬렉션 JOIN FETCH

```java
// ❌ MultipleBagFetchException 발생
@Query("SELECT u FROM User u JOIN FETCH u.habits JOIN FETCH u.badges")
```

**해결:** 하나만 FETCH, 나머지는 LAZY + @BatchSize

---

## 실무 가이드

1. **기본 전략**: 모든 연관관계 `LAZY` 설정
2. **필요시 FETCH**: 명시적으로 JOIN FETCH 사용
3. **성능 모니터링**: 로그로 쿼리 개수 확인

```yaml
# application.yml - 쿼리 로깅
spring:
  jpa:
    properties:
      hibernate:
        format_sql: true
logging:
  level:
    org.hibernate.SQL: debug
```

---

**작성일:** 2025-01-29
