# 오늘한장 API 명세서

## 개요

- **Base URL**: `/api/v1`
- **인증 방식**: JWT (Bearer Token)
- **날짜 형식**: `YYYY-MM-DD`
- **시간 형식**: `ISO 8601`

---

## 1. Auth

인증 관련 API. Refresh Token은 Redis에 저장하며, RTR(Refresh Token Rotation) 방식 적용.

| 기능 | Method | Endpoint | 인증 |
|------|--------|----------|------|
| 회원가입 | POST | `/auth/signup` | X |
| 로그인 | POST | `/auth/login` | X |
| 토큰 재발급 | POST | `/auth/refresh` | X |
| 로그아웃 | POST | `/auth/logout` | O |

### POST /auth/signup

회원가입

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "nickname": "닉네임"
}
```

**Response** `201 Created`
```json
{
  "userId": 1,
  "email": "user@example.com",
  "nickname": "닉네임"
}
```

### POST /auth/login

로그인

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "nickname": "닉네임"
  }
}
```

### POST /auth/refresh

토큰 재발급 (RTR 방식: Refresh Token도 새로 발급)

**Request Body**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### POST /auth/logout

로그아웃 (Redis에서 Refresh Token 삭제)

**Request Body**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response** `204 No Content`

---

## 2. Habit

습관 정의 관리. 시스템 습관(user_id=null)과 커스텀 습관 구분.

| 기능 | Method | Endpoint | 인증 |
|------|--------|----------|------|
| 시스템 습관 목록 조회 | GET | `/habits?type=system` | O |
| 커스텀 습관 생성 | POST | `/habits` | O |
| 커스텀 습관 수정 | PUT | `/habits/{id}` | O |
| 커스텀 습관 삭제 | DELETE | `/habits/{id}` | O |

### GET /habits?type=system

시스템 습관 목록 조회 (연결된 뱃지세트 미리보기 포함)

**Query Parameters**
- `type`: `system` (필수)

**Response** `200 OK`
```json
{
  "habits": [
    {
      "id": 1,
      "name": "달리기",
      "type": "PRACTICE",
      "badgeSets": [
        {
          "id": 1,
          "name": "거리 도전",
          "description": "달린 거리로 뱃지 획득"
        },
        {
          "id": 2,
          "name": "스트릭 도전",
          "description": "연속 달리기 일수로 뱃지 획득"
        }
      ]
    },
    {
      "id": 2,
      "name": "금연",
      "type": "ABSTINENCE",
      "badgeSets": [
        {
          "id": 3,
          "name": "절약 금액 도전",
          "description": "금연으로 절약한 금액 뱃지"
        }
      ]
    }
  ]
}
```

### POST /habits

커스텀 습관 생성

**Request Body**
```json
{
  "name": "명상하기",
  "type": "PRACTICE"
}
```

**Response** `201 Created`
```json
{
  "id": 10,
  "name": "명상하기",
  "type": "PRACTICE",
  "userId": 1
}
```

### PUT /habits/{id}

커스텀 습관 수정 (본인 것만)

**Path Parameters**
- `id`: 습관 ID

**Request Body**
```json
{
  "name": "아침 명상",
  "type": "PRACTICE"
}
```

**Response** `200 OK`
```json
{
  "id": 10,
  "name": "아침 명상",
  "type": "PRACTICE",
  "userId": 1
}
```

### DELETE /habits/{id}

커스텀 습관 삭제 (본인 것만, 연관 데이터 Cascade 삭제)

**Path Parameters**
- `id`: 습관 ID

**Response** `204 No Content`

---

## 3. UserHabit

사용자의 습관 등록/관리. 습관 등록 시 뱃지세트 자동 연결.

| 기능 | Method | Endpoint | 인증 |
|------|--------|----------|------|
| 내 습관 등록 | POST | `/user-habits` | O |
| 내 습관 목록 조회 | GET | `/user-habits` | O |
| 내 습관 상세 조회 | GET | `/user-habits/{id}` | O |
| 내 습관 삭제 | DELETE | `/user-habits/{id}` | O |

### POST /user-habits

내 습관 등록

- 시스템 습관 → 시스템 뱃지세트 + 범용 스트릭 뱃지세트 자동 연결
- 커스텀 습관 → 범용 스트릭 뱃지세트만 자동 연결

**Request Body**
```json
{
  "habitId": 1
}
```

**Response** `201 Created`
```json
{
  "id": 1,
  "habit": {
    "id": 1,
    "name": "달리기",
    "type": "PRACTICE"
  },
  "currentStreak": 0,
  "createdAt": "2025-01-27T10:00:00Z"
}
```

### GET /user-habits

내 습관 목록 조회

**Response** `200 OK`
```json
{
  "userHabits": [
    {
      "id": 1,
      "habit": {
        "id": 1,
        "name": "달리기",
        "type": "PRACTICE"
      },
      "currentStreak": 7,
      "lastCheckedDate": "2025-01-26"
    },
    {
      "id": 2,
      "habit": {
        "id": 2,
        "name": "금연",
        "type": "ABSTINENCE"
      },
      "currentStreak": 30,
      "lastCheckedDate": null
    }
  ]
}
```

### GET /user-habits/{id}

내 습관 상세 조회 (뱃지 진행 상황 포함)

**Path Parameters**
- `id`: UserHabit ID

**Response** `200 OK`
```json
{
  "id": 1,
  "habit": {
    "id": 1,
    "name": "달리기",
    "type": "PRACTICE"
  },
  "currentStreak": 7,
  "lastCheckedDate": "2025-01-26",
  "badgeProgress": [
    {
      "badgeSetName": "스트릭 도전",
      "currentBadge": {
        "name": "7일 연속 달성",
        "icon": "🔥",
        "conditionValue": 7
      },
      "currentValue": 7,
      "progress": 100,
      "nextBadge": {
        "name": "30일 연속 달성",
        "conditionValue": 30
      }
    },
    {
      "badgeSetName": "거리 도전",
      "currentBadge": {
        "name": "하프마라톤 완주",
        "icon": "🏃",
        "conditionValue": 21
      },
      "currentValue": 15,
      "progress": 71,
      "nextBadge": null
    }
  ]
}
```

### DELETE /user-habits/{id}

내 습관 삭제 (연관 데이터 Cascade 삭제)

**Path Parameters**
- `id`: UserHabit ID

**Response** `204 No Content`

---

## 4. HabitLog

습관 체크 기록. 과거 수정은 3일 이내만 허용.

| 기능 | Method | Endpoint | 인증 |
|------|--------|----------|------|
| 습관 체크 | POST | `/user-habits/{id}/logs` | O |
| 체크 취소 | DELETE | `/user-habits/{id}/logs/{date}` | O |
| 특정 습관 로그 조회 | GET | `/user-habits/{id}/logs?date=` | O |
| 날짜별 전체 습관 현황 | GET | `/habit-logs?date=` | O |

### POST /user-habits/{id}/logs

습관 체크 (3일 이내만 가능)

**Path Parameters**
- `id`: UserHabit ID

**Request Body**
```json
{
  "date": "2025-01-27"
}
```
- `date` 생략 시 오늘 날짜

**Response** `201 Created`
```json
{
  "logId": 1,
  "date": "2025-01-27",
  "updatedStreak": 8
}
```

**Error** `400 Bad Request`
```json
{
  "error": "PAST_DATE_LIMIT_EXCEEDED",
  "message": "3일 이전 기록은 수정할 수 없습니다"
}
```

### DELETE /user-habits/{id}/logs/{date}

체크 취소 (3일 이내만 가능)

**Path Parameters**
- `id`: UserHabit ID
- `date`: 날짜 (YYYY-MM-DD)

**Response** `204 No Content`

### GET /user-habits/{id}/logs

특정 습관의 로그 조회

**Path Parameters**
- `id`: UserHabit ID

**Query Parameters**
- `date`: 조회할 날짜 (YYYY-MM-DD)

**Response** `200 OK`
```json
{
  "logId": 1,
  "date": "2025-01-27",
  "checked": true,
  "createdAt": "2025-01-27T22:00:00Z"
}
```

### GET /habit-logs

날짜별 전체 습관 현황 조회 (캘린더용)

**Query Parameters**
- `date`: 조회할 날짜 (YYYY-MM-DD)

**Response** `200 OK`
```json
{
  "date": "2025-01-27",
  "logs": [
    {
      "userHabitId": 1,
      "habitName": "달리기",
      "habitType": "PRACTICE",
      "checked": true
    },
    {
      "userHabitId": 2,
      "habitName": "금연",
      "habitType": "ABSTINENCE",
      "checked": false
    }
  ]
}
```

---

## 5. DailyPage

매일 한 페이지 기록. MVP에서는 1분할 자유 텍스트만 지원.

| 기능 | Method | Endpoint | 인증 |
|------|--------|----------|------|
| 페이지 작성/수정 | PUT | `/daily-pages/{date}` | O |
| 페이지 조회 | GET | `/daily-pages/{date}` | O |
| 페이지 삭제 | DELETE | `/daily-pages/{date}` | O |
| 월별 작성 여부 조회 | GET | `/daily-pages?year=&month=` | O |

### PUT /daily-pages/{date}

페이지 작성/수정 (Upsert)

**Path Parameters**
- `date`: 날짜 (YYYY-MM-DD)

**Request Body**
```json
{
  "content": "오늘 하루도 열심히 살았다. 아침에 달리기를 하고..."
}
```

**Response** `200 OK`
```json
{
  "id": 1,
  "date": "2025-01-27",
  "content": "오늘 하루도 열심히 살았다. 아침에 달리기를 하고...",
  "createdAt": "2025-01-27T22:00:00Z",
  "updatedAt": "2025-01-27T22:30:00Z"
}
```

### GET /daily-pages/{date}

특정 날짜 페이지 조회

**Path Parameters**
- `date`: 날짜 (YYYY-MM-DD)

**Response** `200 OK`
```json
{
  "id": 1,
  "date": "2025-01-27",
  "content": "오늘 하루도 열심히 살았다. 아침에 달리기를 하고...",
  "createdAt": "2025-01-27T22:00:00Z",
  "updatedAt": "2025-01-27T22:30:00Z"
}
```

**Response** `404 Not Found` (해당 날짜에 작성한 페이지 없음)
```json
{
  "error": "PAGE_NOT_FOUND",
  "message": "해당 날짜에 작성한 페이지가 없습니다"
}
```

### DELETE /daily-pages/{date}

페이지 삭제

**Path Parameters**
- `date`: 날짜 (YYYY-MM-DD)

**Response** `204 No Content`

### GET /daily-pages

월별 작성 여부 조회 (캘린더 표시용)

**Query Parameters**
- `year`: 연도 (예: 2025)
- `month`: 월 (예: 1)

**Response** `200 OK`
```json
{
  "year": 2025,
  "month": 1,
  "days": [
    { "date": "2025-01-01", "hasContent": true },
    { "date": "2025-01-02", "hasContent": false },
    { "date": "2025-01-03", "hasContent": true }
  ]
}
```

---

## 6. Badge

뱃지 진행 상황 및 획득 목록 조회.

| 기능 | Method | Endpoint | 인증 |
|------|--------|----------|------|
| 특정 습관 뱃지 진행 현황 | GET | `/user-habits/{id}/badge-sets` | O |
| 내 뱃지세트 전체 진행 현황 | GET | `/user-badge-sets` | O |
| 획득한 뱃지 목록 | GET | `/user-badges` | O |

### GET /user-habits/{id}/badge-sets

특정 습관의 뱃지세트 진행 현황

**Path Parameters**
- `id`: UserHabit ID

**Response** `200 OK`
```json
{
  "badgeSets": [
    {
      "badgeSetName": "스트릭 도전",
      "currentBadge": {
        "name": "7일 연속 달성",
        "icon": "🔥",
        "conditionValue": 7
      },
      "currentValue": 7,
      "progress": 100,
      "nextBadge": {
        "name": "30일 연속 달성",
        "conditionValue": 30
      }
    }
  ]
}
```

### GET /user-badge-sets

내 뱃지세트 전체 진행 현황

**Response** `200 OK`
```json
{
  "badgeSets": [
    {
      "habitName": "달리기",
      "badgeSetName": "스트릭 도전",
      "currentBadge": {
        "name": "7일 연속 달성",
        "icon": "🔥",
        "conditionValue": 7
      },
      "currentValue": 7,
      "progress": 100
    },
    {
      "habitName": "금연",
      "badgeSetName": "절약 금액 도전",
      "currentBadge": {
        "name": "치킨 1마리 값 절약!",
        "icon": "🍗",
        "conditionValue": 1
      },
      "currentValue": 30,
      "progress": 100
    }
  ]
}
```

### GET /user-badges

획득한 뱃지 목록

**Response** `200 OK`
```json
{
  "badges": [
    {
      "badgeName": "7일 연속 달성",
      "badgeIcon": "🔥",
      "habitName": "달리기",
      "completedAt": "2025-01-27T22:00:00Z"
    },
    {
      "badgeName": "치킨 1마리 값 절약!",
      "badgeIcon": "🍗",
      "habitName": "금연",
      "completedAt": "2025-01-20T10:00:00Z"
    }
  ]
}
```

---

## 7. AiFeedback

AI 피드백 (오늘의 한마디). 접속 시 생성되어 저장, 이후 캘린더에서 열람 가능.

| 기능 | Method | Endpoint | 인증 |
|------|--------|----------|------|
| 오늘 피드백 조회 | GET | `/ai-feedback/today` | O |
| 특정 날짜 피드백 조회 | GET | `/ai-feedback/{date}` | O |

### GET /ai-feedback/today

오늘 피드백 조회 (없으면 어제 기록 기반 생성 후 반환)

**Response** `200 OK`
```json
{
  "id": 1,
  "date": "2025-01-27",
  "message": "어제 달리기 7일 연속 성공! 이 페이스 대단해요 💪 금연도 한 달째 유지 중이시네요. 절약한 돈으로 맛있는 거 드세요!",
  "createdAt": "2025-01-27T08:00:00Z"
}
```

### GET /ai-feedback/{date}

특정 날짜 피드백 조회 (해당 날짜에 접속하지 않았으면 null)

**Path Parameters**
- `date`: 날짜 (YYYY-MM-DD)

**Response** `200 OK`
```json
{
  "id": 1,
  "date": "2025-01-27",
  "message": "어제 달리기 7일 연속 성공! 이 페이스 대단해요 💪",
  "createdAt": "2025-01-27T08:00:00Z"
}
```

**Response** `404 Not Found` (해당 날짜에 피드백 없음)
```json
{
  "error": "FEEDBACK_NOT_FOUND",
  "message": "해당 날짜의 피드백이 없습니다"
}
```

---

## ERD 변경사항

### BadgeSet 테이블

```diff
- habit_id bigint [not null, ref: > Habit.id]
+ habit_id bigint [null, ref: > Habit.id, note: 'null이면 범용(습관 무관)']
```

---

## 설계 결정 요약

| 항목 | 결정 | 이유 |
|------|------|------|
| Refresh Token 저장소 | Redis | TTL 자동 만료, 빠른 조회 |
| Token 방식 | RTR | 보안 강화 (탈취 시 1회만 사용 가능) |
| 습관 체크/취소 | POST/DELETE 분리 | REST 원칙 준수, 실수 방지 |
| 과거 수정 기한 | 3일 | 무분별한 수정 방지 |
| DailyPage 작성/수정 | PUT Upsert | 단일 엔드포인트로 간소화 |
| 날짜별 조회 | 별도 API (A방식) | REST 원칙 준수 |
| 범용 뱃지 | BadgeSet.habit_id=null | 모든 습관에 적용 가능한 템플릿 |
| 커스텀 습관 뱃지 | 범용만 연결 | MVP 범위 축소 |
