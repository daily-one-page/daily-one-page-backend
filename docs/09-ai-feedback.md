# 09. AiFeedback API (AI 피드백)

> 커밋: `feat: implement ai-feedback API with mock generation`

---

## 작업 내용

| API | Method | URL | 설명 |
|-----|--------|-----|------|
| 오늘의 피드백 | GET | `/api/ai-feedback/today` | 오늘 피드백 (없으면 자동 생성) |
| 날짜별 피드백 | GET | `/api/ai-feedback?date=` | 특정 날짜 피드백 |
| 히스토리 | GET | `/api/ai-feedback/history?year=&month=` | 월별 피드백 목록 |

---

## 핵심 학습 포인트

### 1. 피드백 생성 흐름

```
사용자 접속 (아침)
       │
       ▼
GET /ai-feedback/today
       │
       ├─ 이미 오늘 피드백 있음? → 바로 반환
       │
       └─ 없음 → 어제 데이터 조회
                    │
                    ├─ HabitLog (어제 습관 체크)
                    └─ DailyPage (어제 작성 페이지)
                    │
                    ▼
              AI 피드백 생성 (Mock)
                    │
                    ▼
              DB 저장 후 반환
```

### 2. 하루에 하나의 피드백만

```java
@Table(uniqueConstraints =
    @UniqueConstraint(columnNames = {"user_id", "date"}))
public class AiFeedback {
```

**설계 의도:**
- 같은 날 여러 번 호출해도 동일한 피드백 반환
- AI API 비용 절약
- 일관된 사용자 경험

### 3. Mock AI 피드백 생성

```java
private String generateMockFeedback(List<HabitLog> habitLogs, DailyPage dailyPage) {
    StringBuilder sb = new StringBuilder();

    if (!habitLogs.isEmpty()) {
        long checkedCount = habitLogs.stream()
                .filter(HabitLog::isChecked)
                .count();

        sb.append(String.format("어제 %d개의 습관을 체크하셨네요! ", checkedCount));

        if (checkedCount == habitLogs.size()) {
            sb.append("모든 습관을 완료하셨습니다. 대단해요! 🎉 ");
        }
    }

    // ...
    return sb.toString();
}
```

**추후 개선:**
```java
// 실제 AI API 연동 시
@Value("${openai.api-key}")
private String apiKey;

private String generateRealFeedback(List<HabitLog> logs, DailyPage page) {
    String prompt = buildPrompt(logs, page);
    return openAiClient.complete(prompt);
}
```

### 4. 데이터 없음 처리

```java
// 어제 데이터가 없으면 피드백 생성 불가
if (yesterdayLogs.isEmpty() && yesterdayPage.isEmpty()) {
    throw new BusinessException(ErrorCode.NO_DATA_FOR_FEEDBACK);
}
```

**왜 이렇게 처리?**
- 피드백은 "어제 활동"을 기반으로 함
- 어제 아무것도 안 했으면 피드백 내용 없음
- 400 에러로 명확히 알림

### 5. Optional 활용

```java
public Optional<AiFeedbackResponse> getLatestFeedback(Long userId) {
    return aiFeedbackRepository.findLatestByUserId(userId)
            .map(AiFeedbackResponse::from);
}
```

**Optional 체이닝:**
```java
Optional<AiFeedback> → .map(변환) → Optional<AiFeedbackResponse>
```

### 6. 히스토리 조회 패턴

```java
public AiFeedbackHistoryResponse getFeedbackHistory(Long userId, int year, int month) {
    YearMonth yearMonth = YearMonth.of(year, month);
    LocalDate startDate = yearMonth.atDay(1);
    LocalDate endDate = yearMonth.atEndOfMonth();

    List<AiFeedback> feedbacks = aiFeedbackRepository
            .findByUserIdAndMonth(userId, startDate, endDate);
    // ...
}
```

**DailyPage 캘린더와 동일한 패턴** → 재사용 가능

---

## 파일 구조

```
domain/ai/
├── controller/
│   └── AiFeedbackController.java (추가)
├── service/
│   └── AiFeedbackService.java (추가)
├── dto/
│   ├── AiFeedbackResponse.java (추가)
│   └── AiFeedbackHistoryResponse.java (추가)
├── entity/
│   └── AiFeedback.java (기존)
└── repository/
    └── AiFeedbackRepository.java (기존)

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
| 1 | `GET /api/ai-feedback/today` (데이터 있음) | 200 OK + 피드백 생성 | ☐ |
| 2 | `GET /api/ai-feedback/today` (재호출) | 200 OK + 같은 피드백 | ☐ |
| 3 | `GET /api/ai-feedback/today` (어제 데이터 없음) | 400 Bad Request | ☐ |
| 4 | `GET /api/ai-feedback?date=...` | 200 OK + 해당 피드백 | ☐ |
| 5 | `GET /api/ai-feedback/history` | 200 OK + 월별 목록 | ☐ |
| 6 | 토큰 없이 호출 | 401 Unauthorized | ☐ |

### 테스트 시나리오

```bash
# 사전 준비: 어제 데이터 필요!
# 1. 어제 날짜로 습관 체크
curl -X POST http://localhost:8080/api/habit-logs \
  -H "Authorization: Bearer {TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"userHabitId": 1, "date": "2025-01-28"}'

# 2. 오늘의 피드백 조회 (자동 생성)
curl http://localhost:8080/api/ai-feedback/today \
  -H "Authorization: Bearer {TOKEN}"
# → 200 + message: "어제 1개의 습관을 체크하셨네요!..."

# 3. 같은 요청 다시 (동일 피드백 반환)
curl http://localhost:8080/api/ai-feedback/today \
  -H "Authorization: Bearer {TOKEN}"
# → 200 + 동일한 피드백

# 4. 어제 데이터 없는 경우
# (새 계정 또는 데이터 삭제 후)
curl http://localhost:8080/api/ai-feedback/today \
  -H "Authorization: Bearer {TOKEN}"
# → 400 NO_DATA_FOR_FEEDBACK

# 5. 월별 히스토리
curl "http://localhost:8080/api/ai-feedback/history?year=2025&month=1" \
  -H "Authorization: Bearer {TOKEN}"
# → 200 + feedbacks 배열
```

---

## 향후 개선 사항

1. **실제 AI API 연동** (OpenAI, Claude 등)
2. **피드백 품질 개선** (더 구체적인 조언)
3. **피드백 유형 다양화** (격려, 분석, 제안 등)
4. **피드백 생성 비동기 처리** (응답 속도 개선)

---

## 관련 학습 자료

- [Spring에서 날짜/시간 다루기](./lectures/spring-datetime.md)

---

**작성일:** 2025-01-29

---

## 🎉 MVP API 개발 완료!

```
✅ Stage 1: Project Setup
✅ Stage 2: Entity & Repository
✅ Stage 3: Auth API
✅ Stage 4: Habit API
✅ Stage 5: UserHabit API
✅ Stage 6: HabitLog API
✅ Stage 7: DailyPage API
✅ Stage 8: Badge API
✅ Stage 9: AiFeedback API
```
