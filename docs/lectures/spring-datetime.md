# Spring에서 날짜/시간 다루기

## Java 8+ 날짜/시간 타입

| 클래스 | 용도 | 예시 |
|--------|------|------|
| `LocalDate` | 날짜만 | 2025-01-29 |
| `LocalTime` | 시간만 | 10:30:00 |
| `LocalDateTime` | 날짜 + 시간 | 2025-01-29T10:30:00 |
| `ZonedDateTime` | 타임존 포함 | 2025-01-29T10:30:00+09:00[Asia/Seoul] |
| `Instant` | UTC 타임스탬프 | 1706520600000 |

---

## Controller에서 날짜 파라미터 받기

### 1. Query Parameter

```java
@GetMapping("/logs")
public ResponseEntity<?> getLogs(
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate date) {
    // date = 2025-01-29
}
```

**요청:** `GET /logs?date=2025-01-29`

### 2. Path Variable

```java
@GetMapping("/logs/{date}")
public ResponseEntity<?> getLogs(
        @PathVariable
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate date) {
    // ...
}
```

**요청:** `GET /logs/2025-01-29`

### 3. Request Body (JSON)

```java
public class LogRequest {
    private LocalDate date;  // "2025-01-29"
    private LocalDateTime timestamp;  // "2025-01-29T10:30:00"
}
```

**Jackson 자동 변환** (추가 설정 불필요)

---

## @DateTimeFormat 옵션

```java
// ISO 표준 형식
@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)       // 2025-01-29
@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  // 2025-01-29T10:30:00

// 커스텀 패턴
@DateTimeFormat(pattern = "yyyy-MM-dd")
@DateTimeFormat(pattern = "yyyy/MM/dd HH:mm:ss")
```

---

## JPA Entity에서 날짜 저장

### 기본 매핑

```java
@Entity
public class HabitLog {
    private LocalDate date;           // DATE 컬럼
    private LocalDateTime createdAt;  // DATETIME 컬럼
}
```

### Auditing으로 자동 생성

```java
@EntityListeners(AuditingEntityListener.class)
public class BaseTimeEntity {
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

---

## 날짜 비교 쿼리

### Spring Data JPA

```java
// 메서드 이름으로 쿼리 생성
List<Log> findByDateBetween(LocalDate start, LocalDate end);
List<Log> findByDateAfter(LocalDate date);
List<Log> findByDateBefore(LocalDate date);
List<Log> findByCreatedAtToday();  // ❌ 지원 안됨

// JPQL로 작성
@Query("SELECT l FROM Log l WHERE l.date = :date")
List<Log> findByDate(@Param("date") LocalDate date);

@Query("SELECT l FROM Log l WHERE l.date BETWEEN :start AND :end")
List<Log> findByDateRange(
    @Param("start") LocalDate start,
    @Param("end") LocalDate end);
```

---

## 타임존 처리

### 문제 상황

```
서버: UTC (협정 세계시)
사용자: Asia/Seoul (UTC+9)

사용자가 "2025-01-29 01:00" 저장
→ DB에 "2025-01-28 16:00" UTC로 저장
→ 조회 시 날짜가 다르게 보임
```

### 해결 방법

**1. 서버 타임존 설정 (application.yml)**
```yaml
spring:
  jackson:
    time-zone: Asia/Seoul
  jpa:
    properties:
      hibernate:
        jdbc:
          time_zone: Asia/Seoul
```

**2. 또는 DB에 UTC 저장, 응답 시 변환**
```java
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
private LocalDateTime createdAt;
```

---

## 오늘 날짜 비교

```java
// 오늘 날짜
LocalDate today = LocalDate.now();

// 어제
LocalDate yesterday = today.minusDays(1);

// 같은 날인지 확인
if (lastCheckedDate.equals(today)) { ... }

// 어제인지 확인
if (lastCheckedDate.equals(today.minusDays(1))) { ... }

// 기간 내인지 확인
if (date.isAfter(startDate) && date.isBefore(endDate)) { ... }
```

---

## 실무 팁

### 1. 날짜 vs 시간 구분

```java
// 날짜만 필요할 때 (체크 날짜, 생일 등)
private LocalDate date;

// 정확한 시점이 필요할 때 (로그, 주문 시간 등)
private LocalDateTime timestamp;
```

### 2. 기본값 설정

```java
public class Request {
    private LocalDate date;

    public LocalDate getDateOrToday() {
        return date != null ? date : LocalDate.now();
    }
}
```

### 3. 요청 시 null 허용

```java
@GetMapping
public ResponseEntity<?> getLogs(
        @RequestParam(required = false) LocalDate date) {

    LocalDate targetDate = date != null ? date : LocalDate.now();
    // ...
}
```

---

**작성일:** 2025-01-29
