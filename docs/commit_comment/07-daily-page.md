# 07. DailyPage API (하루 한 장)

> 커밋: `feat: implement daily-page API for one page per day`

---

## 작업 내용

| API | Method | URL | 설명 |
|-----|--------|-----|------|
| 페이지 작성 | POST | `/api/daily-pages` | 오늘(또는 지정일) 작성 |
| 페이지 조회 | GET | `/api/daily-pages?date=` | 특정 날짜 페이지 |
| 캘린더 조회 | GET | `/api/daily-pages/calendar?year=&month=` | 월별 작성 현황 |
| 페이지 수정 | PUT | `/api/daily-pages/{id}` | 내용 수정 |
| 페이지 삭제 | DELETE | `/api/daily-pages/{id}` | 페이지 삭제 |

---

## 핵심 학습 포인트

### 1. 하루에 하나의 페이지만

```java
@Table(uniqueConstraints =
    @UniqueConstraint(columnNames = {"user_id", "date"}))
public class DailyPage {
```

**DB 레벨에서 중복 방지:**
```sql
-- 같은 사용자 + 같은 날짜 조합은 유일해야 함
UNIQUE INDEX idx_user_date (user_id, date)
```

**서비스 레벨에서도 체크:**
```java
if (dailyPageRepository.existsByUserIdAndDate(userId, date)) {
    throw new BusinessException(ErrorCode.DUPLICATE_PAGE);
}
```

### 2. YearMonth로 월 범위 계산

```java
public CalendarResponse getCalendar(Long userId, int year, int month) {
    YearMonth yearMonth = YearMonth.of(year, month);
    LocalDate startDate = yearMonth.atDay(1);        // 2025-01-01
    LocalDate endDate = yearMonth.atEndOfMonth();    // 2025-01-31

    List<DailyPage> pages = dailyPageRepository
            .findByUserIdAndMonth(userId, startDate, endDate);
    // ...
}
```

**YearMonth 장점:**
- 월의 마지막 날 자동 계산 (28/29/30/31일)
- 윤년 자동 처리

### 3. 미리보기 생성

```java
public static CalendarDay from(Long pageId, LocalDate date, String content) {
    String preview = content.length() > 50
            ? content.substring(0, 50) + "..."
            : content;

    return CalendarDay.builder()
            .date(date)
            .pageId(pageId)
            .preview(preview)
            .build();
}
```

**캘린더에서 전체 내용 대신 미리보기만:**
- 응답 크기 절약
- UI 렌더링 속도 향상
- 상세 내용은 클릭 시 별도 조회

### 4. TEXT 컬럼 타입

```java
@Column(nullable = false, columnDefinition = "TEXT")
private String content;
```

| 타입 | 최대 길이 | 용도 |
|------|----------|------|
| VARCHAR(255) | 255자 | 짧은 텍스트 |
| TEXT | 65,535자 | 긴 본문 |
| MEDIUMTEXT | 16MB | 아주 긴 문서 |
| LONGTEXT | 4GB | 대용량 |

### 5. 기본값 처리 패턴

```java
@Schema(description = "작성 날짜 (null이면 오늘)")
private LocalDate date;

public LocalDate getDateOrToday() {
    return date != null ? date : LocalDate.now();
}
```

**장점:**
- 프론트엔드에서 날짜 안 보내도 됨
- API 사용 편의성 증가
- null 처리 로직 DTO에 캡슐화

---

## 파일 구조

```
domain/dailypage/
├── controller/
│   └── DailyPageController.java (추가)
├── service/
│   └── DailyPageService.java (추가)
├── dto/
│   ├── DailyPageCreateRequest.java (추가)
│   ├── DailyPageUpdateRequest.java (추가)
│   ├── DailyPageResponse.java (추가)
│   └── CalendarResponse.java (추가)
├── entity/
│   └── DailyPage.java (기존)
└── repository/
    └── DailyPageRepository.java (기존)
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
| 1 | `POST /api/daily-pages` | 201 Created | ☐ |
| 2 | `GET /api/daily-pages` (오늘) | 200 OK + 페이지 | ☐ |
| 3 | `GET /api/daily-pages?date=...` | 200 OK + 해당 페이지 | ☐ |
| 4 | `GET /api/daily-pages/calendar` | 200 OK + 월별 목록 | ☐ |
| 5 | `PUT /api/daily-pages/{id}` | 200 OK + 수정된 내용 | ☐ |
| 6 | `DELETE /api/daily-pages/{id}` | 204 No Content | ☐ |
| 7 | 같은 날짜 중복 작성 | 409 Conflict | ☐ |
| 8 | 없는 날짜 조회 | 404 Not Found | ☐ |
| 9 | 남의 페이지 수정/삭제 | 403 Forbidden | ☐ |

### 테스트 시나리오

```bash
# 1. 페이지 작성 ✅
curl -X POST http://localhost:8080/api/daily-pages \
  -H "Authorization: Bearer {TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"content": "오늘 하루도 열심히 달렸다!"}'
# → 201 + pageId

# 2. 중복 작성 시도 ❌
curl -X POST http://localhost:8080/api/daily-pages \
  -H "Authorization: Bearer {TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"content": "다시 쓰기"}'
# → 409 Conflict

# 3. 오늘 페이지 조회 ✅
curl http://localhost:8080/api/daily-pages \
  -H "Authorization: Bearer {TOKEN}"
# → 200 + content

# 4. 캘린더 조회 ✅
curl "http://localhost:8080/api/daily-pages/calendar?year=2025&month=1" \
  -H "Authorization: Bearer {TOKEN}"
# → 200 + days 배열

# 5. 페이지 수정 ✅
curl -X PUT http://localhost:8080/api/daily-pages/1 \
  -H "Authorization: Bearer {TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"content": "수정된 내용!"}'
# → 200 + 수정된 content

# 6. 페이지 삭제 ✅
curl -X DELETE http://localhost:8080/api/daily-pages/1 \
  -H "Authorization: Bearer {TOKEN}"
# → 204 No Content
```

---

## 관련 학습 자료

- [Spring에서 날짜/시간 다루기](../lectures/spring-datetime.md)

---

**작성일:** 2025-01-29
