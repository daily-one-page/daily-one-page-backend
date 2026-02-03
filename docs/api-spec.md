# 오늘한장 API 명세서

> **실제 구현 기준** - 프론트엔드 개발 시 이 문서를 참고하세요.

## 개요

| 항목 | 값 |
|------|---|
| Base URL | `/api` |
| 인증 방식 | JWT Bearer Token |
| 날짜 형식 | `YYYY-MM-DD` (예: 2025-01-30) |
| 시간 형식 | ISO 8601 (예: 2025-01-30T10:00:00) |

## 공통 응답 형식

### 성공 응답

```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

### 에러 응답

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지"
  }
}
```

### 공통 에러 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| `INVALID_INPUT` | 400 | 입력값 검증 실패 |
| `UNAUTHORIZED` | 401 | 인증 필요 |
| `ACCESS_DENIED` | 403 | 접근 권한 없음 |
| `INVALID_TOKEN` | 401 | 유효하지 않은 토큰 |
| `EXPIRED_TOKEN` | 401 | 만료된 토큰 |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류 |

---

## 1. Auth API (인증)

### POST /api/auth/signup

회원가입

**Request**
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
  "success": true,
  "data": 1,
  "error": null
}
```
> `data`는 생성된 사용자 ID

**에러**
| 코드 | 설명 |
|------|------|
| `DUPLICATE_EMAIL` | 이미 존재하는 이메일 |

---

### POST /api/auth/login

로그인

**Request**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 3600000
  },
  "error": null
}
```
> `expiresIn`: Access Token 유효 시간 (밀리초)

**에러**
| 코드 | 설명 |
|------|------|
| `USER_NOT_FOUND` | 존재하지 않는 사용자 |
| `INVALID_PASSWORD` | 비밀번호 불일치 |

---

### POST /api/auth/reissue

토큰 재발급 (RTR: Refresh Token Rotation)

**Request**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 3600000
  },
  "error": null
}
```

**에러**
| 코드 | 설명 |
|------|------|
| `INVALID_TOKEN` | 유효하지 않은 Refresh Token |

---

### POST /api/auth/logout

로그아웃

**Headers**
```
Authorization: Bearer {accessToken}
```

**Request**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response** `204 No Content`

---

## 2. Habit API (습관 정의)

### GET /api/habits

습관 목록 조회

**Headers**
```
Authorization: Bearer {accessToken}
```

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|----------|------|------|
| `type` | X | `system`: 시스템 습관, `custom`: 내 커스텀 습관 |

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "habits": [
      {
        "id": 1,
        "name": "달리기",
        "description": "매일 30분 달리기",
        "icon": "🏃",
        "type": "PRACTICE",
        "isSystem": true
      },
      {
        "id": 2,
        "name": "금연",
        "description": "담배 끊기",
        "icon": "🚭",
        "type": "ABSTINENCE",
        "isSystem": true
      }
    ],
    "totalCount": 2
  },
  "error": null
}
```

---

### POST /api/habits

커스텀 습관 생성

**Headers**
```
Authorization: Bearer {accessToken}
```

**Request**
```json
{
  "name": "독서하기",
  "description": "매일 30분 책 읽기",
  "icon": "📚",
  "type": "PRACTICE"
}
```
> `type`: `PRACTICE`(실천) 또는 `ABSTINENCE`(금지)

**Response** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": 10,
    "name": "독서하기",
    "description": "매일 30분 책 읽기",
    "icon": "📚",
    "type": "PRACTICE",
    "isSystem": false
  },
  "error": null
}
```

---

### PUT /api/habits/{id}

커스텀 습관 수정 (본인 것만)

**Headers**
```
Authorization: Bearer {accessToken}
```

**Request**
```json
{
  "name": "아침 독서",
  "description": "기상 후 30분 책 읽기",
  "icon": "📖",
  "type": "PRACTICE"
}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 10,
    "name": "아침 독서",
    "description": "기상 후 30분 책 읽기",
    "icon": "📖",
    "type": "PRACTICE",
    "isSystem": false
  },
  "error": null
}
```

**에러**
| 코드 | 설명 |
|------|------|
| `HABIT_NOT_FOUND` | 존재하지 않는 습관 |
| `HABIT_NOT_OWNED` | 본인 습관이 아님 |
| `SYSTEM_HABIT_NOT_MODIFIABLE` | 시스템 습관은 수정 불가 |

---

### DELETE /api/habits/{id}

커스텀 습관 삭제 (본인 것만)

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response** `204 No Content`

---

## 3. UserHabit API (내 습관 등록/관리)

### GET /api/user-habits

내 습관 목록 조회

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "userHabits": [
      {
        "id": 1,
        "habitId": 1,
        "habitName": "달리기",
        "habitType": "PRACTICE",
        "currentStreak": 7,
        "lastCheckedDate": "2025-01-29",
        "createdAt": "2025-01-01T10:00:00"
      },
      {
        "id": 2,
        "habitId": 2,
        "habitName": "금연",
        "habitType": "ABSTINENCE",
        "currentStreak": 30,
        "lastCheckedDate": "2025-01-29",
        "createdAt": "2025-01-01T10:00:00"
      }
    ],
    "totalCount": 2
  },
  "error": null
}
```

---

### GET /api/user-habits/{id}

내 습관 상세 조회

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "habit": {
      "id": 1,
      "name": "달리기",
      "description": "매일 30분 달리기",
      "icon": "🏃",
      "type": "PRACTICE"
    },
    "currentStreak": 7,
    "lastCheckedDate": "2025-01-29",
    "createdAt": "2025-01-01T10:00:00"
  },
  "error": null
}
```

**에러**
| 코드 | 설명 |
|------|------|
| `USER_HABIT_NOT_FOUND` | 등록된 습관 없음 |
| `ACCESS_DENIED` | 본인 습관이 아님 |

---

### POST /api/user-habits

습관 등록

**Headers**
```
Authorization: Bearer {accessToken}
```

**Request**
```json
{
  "habitId": 1
}
```

**Response** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "habitId": 1,
    "habitName": "달리기",
    "habitType": "PRACTICE",
    "currentStreak": 0,
    "lastCheckedDate": null,
    "createdAt": "2025-01-30T10:00:00"
  },
  "error": null
}
```

**에러**
| 코드 | 설명 |
|------|------|
| `HABIT_NOT_FOUND` | 존재하지 않는 습관 |
| `DUPLICATE_USER_HABIT` | 이미 등록된 습관 |
| `HABIT_NOT_OWNED` | 타인의 커스텀 습관 |

---

### DELETE /api/user-habits/{id}

습관 해제 (관련 기록도 삭제됨)

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response** `204 No Content`

---

## 4. HabitLog API (습관 체크 기록)

### POST /api/habit-logs

습관 체크

**Headers**
```
Authorization: Bearer {accessToken}
```

**Request**
```json
{
  "userHabitId": 1,
  "date": "2025-01-30",
  "checked": true
}
```
> `date` 생략 시 오늘 날짜

**Response** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userHabitId": 1,
    "habitName": "달리기",
    "date": "2025-01-30",
    "checked": true,
    "currentStreak": 8,
    "createdAt": "2025-01-30T22:00:00"
  },
  "error": null
}
```

**에러**
| 코드 | 설명 |
|------|------|
| `USER_HABIT_NOT_FOUND` | 등록된 습관 없음 |
| `DUPLICATE_HABIT_LOG` | 해당 날짜에 이미 체크됨 |

---

### GET /api/habit-logs

날짜별 습관 현황 조회

**Headers**
```
Authorization: Bearer {accessToken}
```

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|----------|------|------|
| `date` | X | 조회 날짜 (기본값: 오늘) |

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "date": "2025-01-30",
    "logs": [
      {
        "id": 1,
        "userHabitId": 1,
        "habitName": "달리기",
        "habitType": "PRACTICE",
        "checked": true,
        "currentStreak": 8
      },
      {
        "id": null,
        "userHabitId": 2,
        "habitName": "금연",
        "habitType": "ABSTINENCE",
        "checked": false,
        "currentStreak": 30
      }
    ],
    "totalCount": 2
  },
  "error": null
}
```
> `id`가 null이면 해당 날짜에 아직 체크하지 않은 상태

---

### DELETE /api/habit-logs/{id}

습관 체크 취소 (스트릭 재계산됨)

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response** `204 No Content`

**에러**
| 코드 | 설명 |
|------|------|
| `HABIT_LOG_NOT_FOUND` | 존재하지 않는 기록 |
| `ACCESS_DENIED` | 본인 기록이 아님 |

---

## 5. DailyPage API (오늘한장)

### POST /api/daily-pages

페이지 작성

**Headers**
```
Authorization: Bearer {accessToken}
```

**Request**
```json
{
  "content": "오늘 하루도 열심히 살았다. 아침에 달리기를 하고...",
  "date": "2025-01-30"
}
```
> `date` 생략 시 오늘 날짜

**Response** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "date": "2025-01-30",
    "content": "오늘 하루도 열심히 살았다. 아침에 달리기를 하고...",
    "createdAt": "2025-01-30T22:00:00",
    "updatedAt": "2025-01-30T22:00:00"
  },
  "error": null
}
```

**에러**
| 코드 | 설명 |
|------|------|
| `DUPLICATE_PAGE` | 해당 날짜에 이미 페이지 존재 |

---

### GET /api/daily-pages

날짜별 페이지 조회

**Headers**
```
Authorization: Bearer {accessToken}
```

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|----------|------|------|
| `date` | O | 조회 날짜 |

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "date": "2025-01-30",
    "content": "오늘 하루도 열심히 살았다...",
    "createdAt": "2025-01-30T22:00:00",
    "updatedAt": "2025-01-30T22:30:00"
  },
  "error": null
}
```

**에러**
| 코드 | 설명 |
|------|------|
| `PAGE_NOT_FOUND` | 해당 날짜에 페이지 없음 |

---

### PUT /api/daily-pages/{id}

페이지 수정

**Headers**
```
Authorization: Bearer {accessToken}
```

**Request**
```json
{
  "content": "수정된 내용..."
}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "date": "2025-01-30",
    "content": "수정된 내용...",
    "createdAt": "2025-01-30T22:00:00",
    "updatedAt": "2025-01-30T23:00:00"
  },
  "error": null
}
```

---

### DELETE /api/daily-pages/{id}

페이지 삭제

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response** `204 No Content`

---

### GET /api/daily-pages/calendar

월별 캘린더 조회

**Headers**
```
Authorization: Bearer {accessToken}
```

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|----------|------|------|
| `year` | O | 연도 (예: 2025) |
| `month` | O | 월 (예: 1) |

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "year": 2025,
    "month": 1,
    "days": [
      {
        "date": "2025-01-01",
        "hasContent": true,
        "preview": "새해 첫날..."
      },
      {
        "date": "2025-01-02",
        "hasContent": false,
        "preview": null
      }
    ]
  },
  "error": null
}
```

---

## 6. Badge API (뱃지)

### GET /api/badges

전체 뱃지 세트 조회

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "badgeSets": [
      {
        "id": 1,
        "name": "스트릭 도전",
        "description": "연속 달성 일수로 뱃지 획득",
        "badges": [
          {
            "id": 1,
            "name": "7일 연속",
            "description": "7일 연속 달성",
            "icon": "🔥",
            "conditionValue": 7
          },
          {
            "id": 2,
            "name": "30일 연속",
            "description": "30일 연속 달성",
            "icon": "🔥🔥",
            "conditionValue": 30
          }
        ]
      }
    ]
  },
  "error": null
}
```

---

### GET /api/badges/my

내 뱃지 현황 (획득 + 진행 중)

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "acquired": [
      {
        "id": 1,
        "badgeId": 1,
        "badgeName": "7일 연속",
        "badgeIcon": "🔥",
        "habitName": "달리기",
        "acquiredAt": "2025-01-07T10:00:00"
      }
    ],
    "inProgress": [
      {
        "badgeSetName": "스트릭 도전",
        "habitName": "달리기",
        "currentValue": 7,
        "nextBadge": {
          "name": "30일 연속",
          "conditionValue": 30
        },
        "progress": 23
      }
    ]
  },
  "error": null
}
```

---

### GET /api/badges/recent

최근 획득 뱃지 조회

**Headers**
```
Authorization: Bearer {accessToken}
```

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|----------|------|------|
| `limit` | X | 조회 개수 (기본값: 5) |

**Response** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "badgeId": 1,
      "badgeName": "7일 연속",
      "badgeIcon": "🔥",
      "habitName": "달리기",
      "acquiredAt": "2025-01-07T10:00:00"
    }
  ],
  "error": null
}
```

---

### GET /api/badges/sets

시스템 뱃지세트 목록 조회

범용으로 사용 가능한 뱃지세트 목록을 조회합니다.

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "스트릭 도전",
      "description": "연속 달성 일수로 뱃지 획득",
      "badges": [...]
    }
  ],
  "error": null
}
```

---

### GET /api/badges/sets/habit/{habitId}

습관별 적용 가능 뱃지세트 조회

특정 습관에 적용 가능한 뱃지세트 목록을 조회합니다.

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "스트릭 도전",
      "description": "연속 달성 일수로 뱃지 획득",
      "badges": [...]
    }
  ],
  "error": null
}
```

---

## 7. AiFeedback API (AI 피드백)

### GET /api/ai-feedback/today

오늘의 피드백 조회 (없으면 자동 생성)

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "date": "2025-01-30",
    "message": "어제 달리기 7일 연속 성공! 이 페이스 대단해요 💪 금연도 한 달째 유지 중이시네요. 절약한 돈으로 맛있는 거 드세요!",
    "createdAt": "2025-01-30T08:00:00"
  },
  "error": null
}
```

**에러**
| 코드 | 설명 |
|------|------|
| `NO_DATA_FOR_FEEDBACK` | 어제 데이터가 없어 피드백 생성 불가 |

---

### GET /api/ai-feedback

특정 날짜 피드백 조회

**Headers**
```
Authorization: Bearer {accessToken}
```

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|----------|------|------|
| `date` | O | 조회 날짜 |

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "date": "2025-01-30",
    "message": "어제 달리기 7일 연속 성공!...",
    "createdAt": "2025-01-30T08:00:00"
  },
  "error": null
}
```

**에러**
| 코드 | 설명 |
|------|------|
| `FEEDBACK_NOT_FOUND` | 해당 날짜에 피드백 없음 |

---

### GET /api/ai-feedback/history

월별 피드백 히스토리

**Headers**
```
Authorization: Bearer {accessToken}
```

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|----------|------|------|
| `year` | O | 연도 |
| `month` | O | 월 |

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "year": 2025,
    "month": 1,
    "feedbacks": [
      {
        "id": 1,
        "date": "2025-01-30",
        "message": "어제 달리기 7일 연속 성공!...",
        "createdAt": "2025-01-30T08:00:00"
      }
    ],
    "totalCount": 1
  },
  "error": null
}
```

---

## 인증 가이드

### 토큰 사용 방법

1. 로그인 후 `accessToken`과 `refreshToken` 저장
2. API 요청 시 Header에 Access Token 포함:
   ```
   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
   ```
3. Access Token 만료 시 `/api/auth/reissue`로 재발급
4. 새로운 `refreshToken`도 함께 업데이트 (RTR)

### 토큰 만료 처리

```
Access Token 만료 → 401 EXPIRED_TOKEN
  ↓
Refresh Token으로 재발급 요청
  ↓
성공 → 새 토큰으로 원래 요청 재시도
실패 → 로그인 화면으로 이동
```

---

## API 엔드포인트 요약

| 도메인 | Method | Endpoint | 설명 | 인증 |
|--------|--------|----------|------|:----:|
| Auth | POST | /api/auth/signup | 회원가입 | - |
| Auth | POST | /api/auth/login | 로그인 | - |
| Auth | POST | /api/auth/reissue | 토큰 재발급 | - |
| Auth | POST | /api/auth/logout | 로그아웃 | ✓ |
| Habit | GET | /api/habits | 습관 목록 | ✓ |
| Habit | POST | /api/habits | 커스텀 습관 생성 | ✓ |
| Habit | PUT | /api/habits/{id} | 커스텀 습관 수정 | ✓ |
| Habit | DELETE | /api/habits/{id} | 커스텀 습관 삭제 | ✓ |
| UserHabit | GET | /api/user-habits | 내 습관 목록 | ✓ |
| UserHabit | GET | /api/user-habits/{id} | 내 습관 상세 | ✓ |
| UserHabit | POST | /api/user-habits | 습관 등록 | ✓ |
| UserHabit | DELETE | /api/user-habits/{id} | 습관 해제 | ✓ |
| HabitLog | GET | /api/habit-logs | 날짜별 현황 | ✓ |
| HabitLog | POST | /api/habit-logs | 습관 체크 | ✓ |
| HabitLog | DELETE | /api/habit-logs/{id} | 체크 취소 | ✓ |
| DailyPage | GET | /api/daily-pages | 날짜별 조회 | ✓ |
| DailyPage | POST | /api/daily-pages | 페이지 작성 | ✓ |
| DailyPage | PUT | /api/daily-pages/{id} | 페이지 수정 | ✓ |
| DailyPage | DELETE | /api/daily-pages/{id} | 페이지 삭제 | ✓ |
| DailyPage | GET | /api/daily-pages/calendar | 월별 캘린더 | ✓ |
| Badge | GET | /api/badges | 전체 뱃지 세트 | ✓ |
| Badge | GET | /api/badges/my | 내 뱃지 현황 | ✓ |
| Badge | GET | /api/badges/recent | 최근 획득 뱃지 | ✓ |
| Badge | GET | /api/badges/sets | 시스템 뱃지세트 | ✓ |
| Badge | GET | /api/badges/sets/habit/{id} | 습관별 뱃지세트 | ✓ |
| AiFeedback | GET | /api/ai-feedback/today | 오늘 피드백 | ✓ |
| AiFeedback | GET | /api/ai-feedback | 날짜별 피드백 | ✓ |
| AiFeedback | GET | /api/ai-feedback/history | 월별 히스토리 | ✓ |
