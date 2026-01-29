# Spring Data JPA 쿼리 메서드

## 쿼리 메서드란?

메서드 이름만으로 쿼리가 자동 생성되는 기능

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // 메서드 이름 → SQL 자동 생성
    Optional<User> findByEmail(String email);
    // → SELECT * FROM user WHERE email = ?
}
```

---

## 기본 문법

```
find + By + 필드명 + 조건
```

### 예시

```java
// 단일 필드
List<User> findByNickname(String nickname);
// → WHERE nickname = ?

// 여러 조건 (AND)
List<Habit> findByUserIdAndType(Long userId, HabitType type);
// → WHERE user_id = ? AND type = ?

// 여러 조건 (OR)
List<Habit> findByNameOrType(String name, HabitType type);
// → WHERE name = ? OR type = ?
```

---

## 조건 키워드

| 키워드 | 예시 | SQL |
|--------|------|-----|
| `Is`, `Equals` | findByNameIs(name) | `WHERE name = ?` |
| `Not` | findByNameNot(name) | `WHERE name != ?` |
| `IsNull` | findByUserIsNull() | `WHERE user IS NULL` |
| `IsNotNull` | findByUserIsNotNull() | `WHERE user IS NOT NULL` |
| `Like` | findByNameLike(pattern) | `WHERE name LIKE ?` |
| `StartingWith` | findByNameStartingWith(prefix) | `WHERE name LIKE 'prefix%'` |
| `EndingWith` | findByNameEndingWith(suffix) | `WHERE name LIKE '%suffix'` |
| `Containing` | findByNameContaining(word) | `WHERE name LIKE '%word%'` |
| `LessThan` | findByAgeLessThan(age) | `WHERE age < ?` |
| `LessThanEqual` | findByAgeLessThanEqual(age) | `WHERE age <= ?` |
| `GreaterThan` | findByAgeGreaterThan(age) | `WHERE age > ?` |
| `Between` | findByAgeBetween(start, end) | `WHERE age BETWEEN ? AND ?` |
| `In` | findByIdIn(List<Long> ids) | `WHERE id IN (?, ?, ?)` |
| `OrderBy` | findByTypeOrderByNameAsc() | `ORDER BY name ASC` |

---

## 반환 타입

```java
// 단일 결과
User findByEmail(String email);           // 없으면 null
Optional<User> findByEmail(String email); // 없으면 Optional.empty()

// 컬렉션
List<User> findByNickname(String nickname);

// 존재 여부
boolean existsByEmail(String email);
// → SELECT COUNT(*) > 0 FROM user WHERE email = ?

// 개수
long countByType(HabitType type);
// → SELECT COUNT(*) FROM habit WHERE type = ?

// 삭제
void deleteByUserId(Long userId);
// → DELETE FROM habit WHERE user_id = ?
```

---

## @Query 사용

메서드명이 복잡할 때 직접 JPQL 작성

```java
public interface HabitLogRepository extends JpaRepository<HabitLog, Long> {

    // JPQL (엔티티 기준)
    @Query("SELECT hl FROM HabitLog hl WHERE hl.userHabit.id = :userHabitId " +
           "AND hl.date BETWEEN :startDate AND :endDate")
    List<HabitLog> findByPeriod(
            @Param("userHabitId") Long userHabitId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Native SQL (테이블 기준)
    @Query(value = "SELECT * FROM habit_log WHERE checked = true LIMIT :limit",
           nativeQuery = true)
    List<HabitLog> findCheckedLogs(@Param("limit") int limit);
}
```

---

## JOIN FETCH

N+1 문제 해결을 위한 즉시 로딩

```java
// ❌ N+1 문제 발생
List<UserHabit> findByUserId(Long userId);
// 1번: UserHabit 조회
// N번: 각 UserHabit의 Habit 조회

// ✅ JOIN FETCH로 해결
@Query("SELECT uh FROM UserHabit uh " +
       "JOIN FETCH uh.habit " +
       "WHERE uh.user.id = :userId")
List<UserHabit> findByUserIdWithHabit(@Param("userId") Long userId);
// 1번: UserHabit + Habit 함께 조회
```

---

## 페이징

```java
// Pageable 파라미터 추가
Page<DailyPage> findByUserId(Long userId, Pageable pageable);

// 사용
Pageable pageable = PageRequest.of(0, 10, Sort.by("date").descending());
Page<DailyPage> pages = repository.findByUserId(userId, pageable);

pages.getContent();      // 데이터 목록
pages.getTotalElements(); // 전체 개수
pages.getTotalPages();    // 전체 페이지 수
pages.hasNext();          // 다음 페이지 존재 여부
```

---

## 프로젝션

필요한 필드만 조회

```java
// DTO 프로젝션
public interface HabitSummary {
    Long getId();
    String getName();
}

@Query("SELECT h.id as id, h.name as name FROM Habit h")
List<HabitSummary> findAllSummary();

// 또는 생성자 사용
@Query("SELECT new com.example.dto.HabitDto(h.id, h.name) FROM Habit h")
List<HabitDto> findAllDto();
```

---

## 실전 예시 (프로젝트에서 사용)

```java
public interface UserHabitRepository extends JpaRepository<UserHabit, Long> {

    // 사용자의 습관 목록 (습관 정보와 함께)
    @Query("SELECT uh FROM UserHabit uh " +
           "JOIN FETCH uh.habit " +
           "WHERE uh.user.id = :userId")
    List<UserHabit> findByUserIdWithHabit(@Param("userId") Long userId);

    // 중복 체크
    boolean existsByUserIdAndHabitId(Long userId, Long habitId);

    // 사용자의 특정 습관 조회
    Optional<UserHabit> findByUserIdAndHabitId(Long userId, Long habitId);

    // 사용자의 습관 개수
    long countByUserId(Long userId);
}
```

---

## 주의사항

1. **메서드명이 너무 길어지면 @Query 사용**
   ```java
   // ❌ 너무 긺
   findByUserIdAndDateBetweenAndCheckedTrueOrderByDateDesc(...)

   // ✅ @Query 사용
   @Query("SELECT hl FROM HabitLog hl WHERE ...")
   List<HabitLog> findCheckedLogsByPeriod(...);
   ```

2. **LIMIT 사용 시 주의**
   ```java
   // JPA 표준 (하이버네이트 6+)
   @Query("... ORDER BY date DESC LIMIT :limit")

   // 또는 Pageable 사용
   Pageable pageable = PageRequest.of(0, limit);
   ```

3. **동적 쿼리는 QueryDSL 고려**
   - 조건이 많고 선택적일 때
   - 복잡한 조인이 필요할 때

---

**작성일:** 2025-01-29
