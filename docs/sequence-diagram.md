# 오늘한장 시퀀스 다이어그램

## 개요

주요 기능별 시퀀스 다이어그램을 PlantUML 형식으로 정리한 문서.

---

## 1. Auth

### 1.1 회원가입

```plantuml
@startuml 회원가입
actor User
participant Client
participant Server
database MySQL

User -> Client: 회원가입 정보 입력\n(email, password, nickname)
Client -> Server: POST /api/auth/signup
Server -> MySQL: 이메일 중복 확인
MySQL --> Server: 결과

alt 이메일 중복
    Server --> Client: 409 Conflict\n"이미 존재하는 이메일"
    Client --> User: 에러 표시
else 이메일 사용 가능
    Server -> Server: 비밀번호 암호화 (BCrypt)
    Server -> MySQL: User INSERT
    MySQL --> Server: 저장 완료
    Server --> Client: 201 Created\n{userId, email, nickname}
    Client --> User: 가입 완료 화면
end

@enduml
```

### 1.2 로그인

```plantuml
@startuml 로그인
actor User
participant Client
participant Server
database MySQL
database Redis

User -> Client: 로그인 정보 입력\n(email, password)
Client -> Server: POST /api/auth/login
Server -> MySQL: 이메일로 User 조회
MySQL --> Server: User 정보

alt 사용자 없음
    Server --> Client: 401 Unauthorized\n"이메일 또는 비밀번호 오류"
    Client --> User: 에러 표시
else 사용자 존재
    Server -> Server: 비밀번호 검증 (BCrypt)
    
    alt 비밀번호 불일치
        Server --> Client: 401 Unauthorized\n"이메일 또는 비밀번호 오류"
        Client --> User: 에러 표시
    else 비밀번호 일치
        Server -> Server: Access Token 생성 (짧은 만료)
        Server -> Server: Refresh Token 생성 (긴 만료)
        Server -> Redis: Refresh Token 저장\n(KEY: userId, TTL: 14일)
        Redis --> Server: 저장 완료
        Server --> Client: 200 OK\n{accessToken, refreshToken, user}
        Client -> Client: 토큰 저장
        Client --> User: 메인 화면 이동
    end
end

@enduml
```

### 1.3 토큰 재발급 (RTR)

```plantuml
@startuml 토큰 재발급
actor User
participant Client
participant Server
database Redis

Client -> Server: POST /api/auth/reissue\n{refreshToken}
Server -> Server: Refresh Token 검증 (서명, 만료)

alt 토큰 유효하지 않음
    Server --> Client: 401 Unauthorized\n"유효하지 않은 토큰"
    Client --> User: 로그인 화면 이동
else 토큰 유효
    Server -> Redis: 저장된 Refresh Token 확인\n(KEY: userId)
    Redis --> Server: 저장된 토큰
    
    alt 저장된 토큰과 불일치 (탈취 의심)
        Server -> Redis: 해당 유저 토큰 삭제
        Redis --> Server: 삭제 완료
        Server --> Client: 401 Unauthorized\n"토큰 재사용 감지"
        Client --> User: 로그인 화면 이동
    else 토큰 일치
        Server -> Server: 새 Access Token 생성
        Server -> Server: 새 Refresh Token 생성 (RTR)
        Server -> Redis: 기존 토큰 삭제 + 새 토큰 저장
        Redis --> Server: 저장 완료
        Server --> Client: 200 OK\n{accessToken, refreshToken}
        Client -> Client: 토큰 갱신
    end
end

@enduml
```

### 1.4 로그아웃

```plantuml
@startuml 로그아웃
actor User
participant Client
participant Server
database Redis

User -> Client: 로그아웃 버튼 클릭
Client -> Server: POST /api/auth/logout\n{refreshToken}
Server -> Server: Access Token에서 userId 추출
Server -> Redis: Refresh Token 삭제\n(KEY: userId)
Redis --> Server: 삭제 완료
Server --> Client: 204 No Content
Client -> Client: 저장된 토큰 삭제
Client --> User: 로그인 화면 이동

@enduml
```

---

## 2. Habit

### 2.1 시스템 습관 목록 조회

```plantuml
@startuml 시스템 습관 조회
actor User
participant Client
participant Server
database MySQL

User -> Client: 습관 추가 화면 진입
Client -> Server: GET /api/habits?type=system\n[Authorization: Bearer token]
Server -> MySQL: 시스템 습관 조회\n(user_id IS NULL)
MySQL --> Server: 습관 목록

loop 각 습관별
    Server -> MySQL: 연결된 BadgeSet 조회
    MySQL --> Server: BadgeSet 목록
end

Server --> Client: 200 OK\n{habits: [{id, name, type, badgeSets}]}
Client --> User: 시스템 습관 목록 표시

@enduml
```

### 2.2 커스텀 습관 생성

```plantuml
@startuml 커스텀 습관 생성
actor User
participant Client
participant Server
database MySQL

User -> Client: 커스텀 습관 입력\n(name, type)
Client -> Server: POST /api/habits\n{name, type}
Server -> MySQL: Habit INSERT\n(user_id = 현재유저)
MySQL --> Server: 저장 완료
Server --> Client: 201 Created\n{id, name, type, userId}
Client --> User: 생성 완료 표시

@enduml
```

### 2.3 커스텀 습관 수정

```plantuml
@startuml 커스텀 습관 수정
actor User
participant Client
participant Server
database MySQL

User -> Client: 습관 수정 입력\n(name, type)
Client -> Server: PUT /api/habits/{id}\n{name, type}
Server -> MySQL: Habit 조회
MySQL --> Server: Habit 정보

alt 본인 습관이 아님 (user_id 불일치)
    Server --> Client: 403 Forbidden\n"수정 권한 없음"
    Client --> User: 에러 표시
else 본인 습관
    Server -> MySQL: Habit UPDATE
    MySQL --> Server: 수정 완료
    Server --> Client: 200 OK\n{id, name, type, userId}
    Client --> User: 수정 완료 표시
end

@enduml
```

### 2.4 커스텀 습관 삭제

```plantuml
@startuml 커스텀 습관 삭제
actor User
participant Client
participant Server
database MySQL
database Redis

User -> Client: 습관 삭제 확인
Client -> Server: DELETE /api/habits/{id}
Server -> MySQL: Habit 조회
MySQL --> Server: Habit 정보

alt 본인 습관이 아님
    Server --> Client: 403 Forbidden\n"삭제 권한 없음"
    Client --> User: 에러 표시
else 본인 습관
    Server -> MySQL: CASCADE 삭제 시작
    Server -> MySQL: UserBadge 삭제
    Server -> MySQL: UserBadgeSet 삭제
    Server -> MySQL: HabitLog 삭제
    Server -> MySQL: UserHabit 삭제
    Server -> MySQL: Habit 삭제
    MySQL --> Server: 삭제 완료
    Server -> Redis: 관련 스트릭 캐시 삭제
    Redis --> Server: 삭제 완료
    Server --> Client: 204 No Content
    Client --> User: 삭제 완료 표시
end

@enduml
```

---

## 3. UserHabit

### 3.1 시스템 습관 등록

```plantuml
@startuml 시스템 습관 등록
actor User
participant Client
participant Server
database MySQL

User -> Client: 시스템 습관 선택
Client -> Server: POST /api/user-habits\n{habitId}
Server -> MySQL: Habit 조회
MySQL --> Server: Habit 정보 (시스템 습관)

Server -> MySQL: UserHabit INSERT\n(current_streak=0)
MySQL --> Server: UserHabit 생성됨

== 시스템 뱃지세트 자동 연결 ==
Server -> MySQL: 해당 습관의 시스템 BadgeSet 조회\n(habit_id = habitId, user_id IS NULL)
MySQL --> Server: 시스템 BadgeSet 목록

loop 각 시스템 BadgeSet
    Server -> MySQL: BadgeSet의 첫 번째 Badge 조회\n(sequence = 1)
    MySQL --> Server: 첫 번째 Badge
    Server -> MySQL: UserBadgeSet INSERT\n(current_badge_id = 첫 번째 Badge)
    MySQL --> Server: 저장 완료
end

== 범용 뱃지세트 자동 연결 ==
Server -> MySQL: 범용 BadgeSet 조회\n(habit_id IS NULL, user_id IS NULL)
MySQL --> Server: 범용 BadgeSet 목록

loop 각 범용 BadgeSet
    Server -> MySQL: BadgeSet의 첫 번째 Badge 조회
    MySQL --> Server: 첫 번째 Badge
    Server -> MySQL: UserBadgeSet INSERT
    MySQL --> Server: 저장 완료
end

Server --> Client: 201 Created\n{id, habit, currentStreak}
Client --> User: 습관 등록 완료

@enduml
```

### 3.2 커스텀 습관 등록

```plantuml
@startuml 커스텀 습관 등록
actor User
participant Client
participant Server
database MySQL

User -> Client: 커스텀 습관 선택
Client -> Server: POST /api/user-habits\n{habitId}
Server -> MySQL: Habit 조회
MySQL --> Server: Habit 정보 (커스텀 습관)

Server -> MySQL: UserHabit INSERT\n(current_streak=0)
MySQL --> Server: UserHabit 생성됨

== 범용 뱃지세트만 자동 연결 ==
Server -> MySQL: 범용 BadgeSet 조회\n(habit_id IS NULL, user_id IS NULL)
MySQL --> Server: 범용 BadgeSet 목록

loop 각 범용 BadgeSet
    Server -> MySQL: BadgeSet의 첫 번째 Badge 조회
    MySQL --> Server: 첫 번째 Badge
    Server -> MySQL: UserBadgeSet INSERT
    MySQL --> Server: 저장 완료
end

Server --> Client: 201 Created\n{id, habit, currentStreak}
Client --> User: 습관 등록 완료

note right of Server
  커스텀 습관은 시스템 뱃지세트가 없으므로
  범용 스트릭 뱃지세트만 연결됨
end note

@enduml
```

### 3.3 내 습관 목록 조회

```plantuml
@startuml 내 습관 목록 조회
actor User
participant Client
participant Server
database MySQL
database Redis

User -> Client: 습관 목록 화면 진입
Client -> Server: GET /api/user-habits
Server -> MySQL: UserHabit 목록 조회\n(user_id = 현재유저)
MySQL --> Server: UserHabit 목록

loop 각 UserHabit별
    Server -> Redis: 스트릭 캐시 조회\n(KEY: streak:user:{userId}:habit:{habitId})
    
    alt 캐시 HIT
        Redis --> Server: currentStreak
    else 캐시 MISS
        Server -> MySQL: UserHabit.current_streak 조회
        MySQL --> Server: currentStreak
        Server -> Redis: 캐시 저장
        Redis --> Server: 저장 완료
    end
end

Server --> Client: 200 OK\n{userHabits: [{id, habit, currentStreak, lastCheckedDate}]}
Client --> User: 습관 목록 표시

@enduml
```

### 3.4 내 습관 상세 조회

```plantuml
@startuml 내 습관 상세 조회
actor User
participant Client
participant Server
database MySQL
database Redis

User -> Client: 습관 상세 화면 진입
Client -> Server: GET /api/user-habits/{id}
Server -> MySQL: UserHabit 조회
MySQL --> Server: UserHabit 정보

Server -> Redis: 스트릭 캐시 조회
alt 캐시 HIT
    Redis --> Server: currentStreak
else 캐시 MISS
    Server -> MySQL: UserHabit.current_streak 조회
    MySQL --> Server: currentStreak
    Server -> Redis: 캐시 저장
end

== 뱃지 진행 상황 조회 ==
Server -> MySQL: UserBadgeSet 목록 조회\n(user_habit_id = id)
MySQL --> Server: UserBadgeSet 목록

loop 각 UserBadgeSet별
    Server -> MySQL: BadgeSet 정보 조회
    Server -> MySQL: current_badge 정보 조회
    Server -> MySQL: next_badge 조회\n(sequence = current + 1)
    MySQL --> Server: 뱃지 정보
end

Server --> Client: 200 OK\n{id, habit, currentStreak, badgeProgress[]}
Client --> User: 습관 상세 + 뱃지 진행 표시

@enduml
```

### 3.5 내 습관 삭제

```plantuml
@startuml 내 습관 삭제
actor User
participant Client
participant Server
database MySQL
database Redis

User -> Client: 내 습관 삭제 확인
Client -> Server: DELETE /api/user-habits/{id}
Server -> MySQL: UserHabit 조회
MySQL --> Server: UserHabit 정보

alt 본인 습관이 아님
    Server --> Client: 403 Forbidden
    Client --> User: 에러 표시
else 본인 습관
    Server -> MySQL: CASCADE 삭제 시작
    Server -> MySQL: UserBadge 삭제
    Server -> MySQL: UserBadgeSet 삭제
    Server -> MySQL: HabitLog 삭제
    Server -> MySQL: UserHabit 삭제
    MySQL --> Server: 삭제 완료
    Server -> Redis: 스트릭 캐시 삭제
    Redis --> Server: 삭제 완료
    Server --> Client: 204 No Content
    Client --> User: 삭제 완료 표시
end

@enduml
```

---

## 4. HabitLog

### 4.1 오늘 습관 체크

```plantuml
@startuml 오늘 습관 체크
actor User
participant Client
participant Server
database MySQL
database Redis

User -> Client: 습관 체크 버튼 클릭
Client -> Server: POST /api/habit-logs\n{userHabitId, date: "2025-01-27", checked: true}
Server -> Server: 날짜 검증 (3일 이내)

alt 3일 초과
    Server --> Client: 400 Bad Request\n"3일 이전 기록은 수정 불가"
    Client --> User: 에러 표시
else 3일 이내
    Server -> MySQL: HabitLog 중복 확인
    MySQL --> Server: 기존 로그 여부
    
    alt 이미 체크됨
        Server --> Client: 409 Conflict\n"이미 체크된 날짜"
        Client --> User: 에러 표시
    else 체크 가능
        Server -> MySQL: BEGIN TRANSACTION
        
        == HabitLog 저장 ==
        Server -> MySQL: HabitLog INSERT\n(checked = true)
        MySQL --> Server: 저장 완료
        
        == 스트릭 업데이트 ==
        Server -> MySQL: UserHabit 조회\n(last_checked_date, current_streak)
        MySQL --> Server: 현재 스트릭 정보
        
        alt last_checked_date == 어제
            Server -> Server: current_streak += 1
        else last_checked_date != 어제
            Server -> Server: current_streak = 1
        end
        
        Server -> MySQL: UserHabit UPDATE\n(current_streak, last_checked_date)
        MySQL --> Server: 업데이트 완료
        
        == 뱃지 진행 업데이트 ==
        Server -> MySQL: UserBadgeSet 목록 조회
        MySQL --> Server: UserBadgeSet 목록
        
        loop 각 UserBadgeSet
            Server -> Server: current_value += 1
            Server -> MySQL: current_badge의 condition_value 조회
            MySQL --> Server: 달성 조건
            
            alt current_value >= condition_value (뱃지 달성!)
                Server -> MySQL: UserBadge INSERT\n(completed_at = now)
                MySQL --> Server: 뱃지 획득 저장
                Server -> MySQL: 다음 Badge 조회\n(sequence + 1)
                MySQL --> Server: 다음 Badge
                
                alt 다음 Badge 있음
                    Server -> MySQL: UserBadgeSet UPDATE\n(current_badge_id = 다음, current_value = 0)
                else 세트 완료
                    Server -> MySQL: UserBadgeSet UPDATE\n(current_value 유지, 완료 상태)
                end
            else 아직 미달성
                Server -> MySQL: UserBadgeSet UPDATE\n(current_value)
            end
            MySQL --> Server: 업데이트 완료
        end
        
        Server -> MySQL: COMMIT
        
        == 캐시 갱신 (Write-Through) ==
        Server -> Redis: SET streak:user:{userId}:habit:{habitId}\n= new_streak
        Redis --> Server: 저장 완료
        
        Server --> Client: 201 Created\n{logId, date, updatedStreak}
        Client --> User: 체크 완료 + 스트릭 표시
    end
end

@enduml
```

### 4.2 과거 습관 수정 (체크)

```plantuml
@startuml 과거 습관 수정
actor User
participant Client
participant Server
database MySQL
database Redis

User -> Client: 과거 날짜 체크
Client -> Server: POST /api/habit-logs\n{userHabitId, date: "2025-01-25", checked: true}
Server -> Server: 날짜 검증 (3일 이내)

alt 3일 초과
    Server --> Client: 400 Bad Request
    Client --> User: 에러 표시
else 3일 이내
    Server -> MySQL: BEGIN TRANSACTION
    
    == HabitLog 저장 ==
    Server -> MySQL: HabitLog INSERT
    MySQL --> Server: 저장 완료
    
    == 스트릭 전체 재계산 ==
    Server -> MySQL: 해당 습관의 모든 HabitLog 조회\n(ORDER BY date DESC)
    MySQL --> Server: 전체 로그 목록
    
    Server -> Server: 연속 일수 재계산\n(오늘부터 역순으로 연속 체크 카운트)
    
    Server -> MySQL: UserHabit UPDATE\n(current_streak = 재계산값)
    MySQL --> Server: 업데이트 완료
    
    == 뱃지 진행 재계산 ==
    note right of Server
      과거 수정 시에는 뱃지 진행도
      전체 재계산이 필요할 수 있음
      (복잡도 고려하여 MVP에서는
      스트릭 뱃지만 재계산)
    end note
    
    Server -> MySQL: COMMIT
    
    == 캐시 무효화 (Cache Invalidation) ==
    Server -> Redis: DEL streak:user:{userId}:habit:{habitId}
    Redis --> Server: 삭제 완료
    
    Server --> Client: 201 Created\n{logId, date, updatedStreak}
    Client --> User: 수정 완료 표시
end

@enduml
```

### 4.3 습관 체크 취소

```plantuml
@startuml 습관 체크 취소
actor User
participant Client
participant Server
database MySQL
database Redis

User -> Client: 체크 취소 버튼 클릭
Client -> Server: DELETE /api/habit-logs/{logId}
Server -> Server: 날짜 검증 (3일 이내)

alt 3일 초과
    Server --> Client: 400 Bad Request
    Client --> User: 에러 표시
else 3일 이내
    Server -> MySQL: HabitLog 조회
    MySQL --> Server: HabitLog 정보
    
    alt 로그 없음
        Server --> Client: 404 Not Found
        Client --> User: 에러 표시
    else 로그 존재
        Server -> MySQL: BEGIN TRANSACTION
        
        Server -> MySQL: HabitLog DELETE
        MySQL --> Server: 삭제 완료
        
        == 스트릭 재계산 ==
        Server -> MySQL: 전체 HabitLog 조회
        MySQL --> Server: 로그 목록
        Server -> Server: 연속 일수 재계산
        Server -> MySQL: UserHabit UPDATE
        MySQL --> Server: 업데이트 완료
        
        Server -> MySQL: COMMIT
        
        == 캐시 무효화 ==
        Server -> Redis: DEL streak:user:{userId}:habit:{habitId}
        Redis --> Server: 삭제 완료
        
        Server --> Client: 204 No Content
        Client --> User: 취소 완료 표시
    end
end

@enduml
```

### 4.4 날짜별 전체 습관 현황 조회

```plantuml
@startuml 날짜별 습관 현황 조회
actor User
participant Client
participant Server
database MySQL

User -> Client: 캘린더에서 날짜 선택
Client -> Server: GET /api/habit-logs?date=2025-01-27
Server -> MySQL: 해당 유저의 모든 UserHabit 조회
MySQL --> Server: UserHabit 목록

loop 각 UserHabit별
    Server -> MySQL: HabitLog 조회\n(user_habit_id, date)
    MySQL --> Server: HabitLog (있으면 checked, 없으면 null)
end

Server --> Client: 200 OK\n{date, logs: [{userHabitId, habitName, habitType, checked}]}
Client --> User: 해당 날짜 습관 현황 표시

@enduml
```

---

## 5. DailyPage

### 5.1 페이지 작성

```plantuml
@startuml 페이지 작성
actor User
participant Client
participant Server
database MySQL

User -> Client: 페이지 내용 작성
Client -> Server: POST /api/daily-pages\n{content, date}
Server -> MySQL: DailyPage 중복 확인\n(user_id, date)
MySQL --> Server: 기존 페이지 여부

alt 해당 날짜에 페이지 존재
    Server --> Client: 409 Conflict\nDUPLICATE_PAGE
    Client --> User: 에러 표시
else 페이지 없음
    Server -> MySQL: DailyPage INSERT
    MySQL --> Server: 저장 완료
    Server --> Client: 201 Created\n{id, date, content, createdAt, updatedAt}
    Client --> User: 저장 완료 표시
end

@enduml
```

### 5.2 페이지 수정

```plantuml
@startuml 페이지 수정
actor User
participant Client
participant Server
database MySQL

User -> Client: 페이지 내용 수정
Client -> Server: PUT /api/daily-pages/{id}\n{content}
Server -> MySQL: DailyPage 조회\n(id, user_id 검증)
MySQL --> Server: DailyPage 정보

alt 페이지 없음 또는 본인 것 아님
    Server --> Client: 403/404 Error
    Client --> User: 에러 표시
else 본인 페이지
    Server -> MySQL: DailyPage UPDATE\n(content, updated_at)
    MySQL --> Server: 수정 완료
    Server --> Client: 200 OK\n{id, date, content, createdAt, updatedAt}
    Client --> User: 수정 완료 표시
end

@enduml
```

### 5.3 페이지 조회

```plantuml
@startuml 페이지 조회
actor User
participant Client
participant Server
database MySQL

User -> Client: 특정 날짜 페이지 열람
Client -> Server: GET /api/daily-pages?date=2025-01-27
Server -> MySQL: DailyPage 조회\n(user_id, date)
MySQL --> Server: DailyPage 정보

alt 페이지 없음
    Server --> Client: 404 Not Found\nPAGE_NOT_FOUND
    Client --> User: 빈 페이지 표시 (작성 유도)
else 페이지 있음
    Server --> Client: 200 OK\n{id, date, content, createdAt, updatedAt}
    Client --> User: 페이지 내용 표시
end

@enduml
```

### 5.4 페이지 삭제

```plantuml
@startuml 페이지 삭제
actor User
participant Client
participant Server
database MySQL

User -> Client: 페이지 삭제 확인
Client -> Server: DELETE /api/daily-pages/{id}
Server -> MySQL: DailyPage 조회\n(id, user_id 검증)
MySQL --> Server: DailyPage 정보

alt 페이지 없음 또는 본인 것 아님
    Server --> Client: 403/404 Error
    Client --> User: 에러 표시
else 본인 페이지
    Server -> MySQL: DailyPage DELETE
    MySQL --> Server: 삭제 완료
    Server --> Client: 204 No Content
    Client --> User: 삭제 완료 표시
end

@enduml
```

### 5.5 월별 캘린더 조회

```plantuml
@startuml 월별 캘린더 조회
actor User
participant Client
participant Server
database MySQL

User -> Client: 캘린더 화면 진입\n(2025년 1월)
Client -> Server: GET /api/daily-pages/calendar?year=2025&month=1
Server -> MySQL: 해당 월의 DailyPage 조회\n(user_id, date BETWEEN 1/1 AND 1/31)
MySQL --> Server: DailyPage 목록

Server -> Server: 각 날짜별 hasContent, preview 매핑

Server --> Client: 200 OK\n{year, month, days: [{date, hasContent, preview}]}
Client --> User: 캘린더에 작성 여부 표시\n(● 작성함 / ○ 미작성)

@enduml
```

---

## 6. Badge

### 6.1 전체 뱃지세트 조회

```plantuml
@startuml 전체 뱃지세트 조회
actor User
participant Client
participant Server
database MySQL

User -> Client: 뱃지 목록 화면 진입
Client -> Server: GET /api/badges
Server -> MySQL: 모든 BadgeSet 조회
MySQL --> Server: BadgeSet 목록

loop 각 BadgeSet별
    Server -> MySQL: Badge 목록 조회\n(badge_set_id)
    MySQL --> Server: Badge 목록
end

Server --> Client: 200 OK\n{badgeSets: [{id, name, description, badges: [...]}]}
Client --> User: 전체 뱃지세트 표시

@enduml
```

### 6.2 내 뱃지 현황 조회

```plantuml
@startuml 내 뱃지 현황 조회
actor User
participant Client
participant Server
database MySQL

User -> Client: 내 뱃지 화면 진입
Client -> Server: GET /api/badges/my
Server -> MySQL: UserBadge 목록 조회\n(user_id = 현재유저)
MySQL --> Server: 획득한 뱃지 목록

Server -> MySQL: UserBadgeSet 목록 조회\n(user_id = 현재유저)
MySQL --> Server: 진행 중인 뱃지세트 목록

loop 각 UserBadgeSet별
    Server -> MySQL: BadgeSet, current_badge 정보 조회
    Server -> MySQL: UserHabit → Habit 정보 조회 (habitName)
    MySQL --> Server: 관련 정보
    Server -> Server: progress 계산\n(currentValue / conditionValue * 100)
end

Server --> Client: 200 OK\n{acquired: [...], inProgress: [...]}
Client --> User: 획득 뱃지 + 진행 현황 표시

@enduml
```

### 6.3 최근 획득 뱃지 조회

```plantuml
@startuml 최근 획득 뱃지 조회
actor User
participant Client
participant Server
database MySQL

User -> Client: 최근 뱃지 확인
Client -> Server: GET /api/badges/recent?limit=5
Server -> MySQL: UserBadge 최근 조회\n(user_id, ORDER BY completed_at DESC, LIMIT 5)
MySQL --> Server: UserBadge 목록

loop 각 UserBadge별
    Server -> MySQL: Badge 정보 조회 (name, icon)
    Server -> MySQL: UserBadgeSet → UserHabit → Habit 조회 (habitName)
    MySQL --> Server: 관련 정보
end

Server --> Client: 200 OK\n[{badgeName, badgeIcon, habitName, acquiredAt}]
Client --> User: 최근 획득 뱃지 표시

@enduml
```

---

## 7. AiFeedback

### 7.1 오늘 피드백 조회 (없으면 생성)

```plantuml
@startuml 오늘 피드백 조회
actor User
participant Client
participant Server
database MySQL
participant OpenAI

User -> Client: 앱 실행 (오늘 첫 접속)
Client -> Server: GET /api/ai-feedback/today
Server -> MySQL: 오늘 AiFeedback 조회\n(user_id, date = today)
MySQL --> Server: AiFeedback (있으면)

alt 오늘 피드백 이미 존재
    Server --> Client: 200 OK\n{id, date, message, createdAt}
    Client --> User: 피드백 표시
else 오늘 피드백 없음 (생성 필요)
    == 어제 기록 수집 ==
    Server -> MySQL: 어제 HabitLog 조회\n(date = yesterday)
    MySQL --> Server: 어제 습관 체크 현황
    
    Server -> MySQL: 어제 DailyPage 조회\n(date = yesterday)
    MySQL --> Server: 어제 작성 내용
    
    Server -> MySQL: UserHabit 스트릭 정보 조회
    MySQL --> Server: 스트릭 정보
    
    == AI 피드백 생성 ==
    Server -> Server: 프롬프트 구성\n(습관 현황 + 스트릭 + 페이지 내용)
    Server -> OpenAI: 피드백 생성 요청
    OpenAI --> Server: AI 응답 (message)
    
    == 저장 ==
    Server -> MySQL: AiFeedback INSERT\n(date = today, message)
    MySQL --> Server: 저장 완료
    
    Server --> Client: 200 OK\n{id, date, message, createdAt}
    Client --> User: 피드백 표시\n"어제 운동 7일 연속 성공! 💪"
end

@enduml
```

### 7.2 특정 날짜 피드백 조회

```plantuml
@startuml 특정 날짜 피드백 조회
actor User
participant Client
participant Server
database MySQL

User -> Client: 캘린더에서 과거 날짜 선택
Client -> Server: GET /api/ai-feedback?date=2025-01-25
Server -> MySQL: AiFeedback 조회\n(user_id, date)
MySQL --> Server: AiFeedback 정보

alt 피드백 없음 (그날 접속 안 함)
    Server --> Client: 404 Not Found
    Client --> User: "해당 날짜의 피드백이 없습니다"
else 피드백 있음
    Server --> Client: 200 OK\n{id, date, message, createdAt}
    Client --> User: 과거 피드백 표시
end

@enduml
```

---

## 8. 통합 시나리오

### 8.1 캘린더 날짜 선택 시 통합 조회

```plantuml
@startuml
title Calendar Date Selection
actor User
participant Client
participant Server
database MySQL

User -> Client: Select January 25th from Calendar
== Parallel API Calls ==
par Habit Status
    Client -> Server: GET /api/habit-logs?date=2025-01-25
    Server -> MySQL: Query HabitLog
    MySQL --> Server: Habit Status
    Server --> Client: {logs: [...]}
else Daily Pages
    Client -> Server: GET /api/daily-pages?date=2025-01-25
    Server -> MySQL: Query DailyPage
    MySQL --> Server: Page Content
    Server --> Client: {content: "..."}
else AI Feedback
    Client -> Server: GET /api/ai-feedback?date=2025-01-25
    Server -> MySQL: Query AiFeedback
    MySQL --> Server: Feedback
    Server --> Client: {message: "..."}
end

Client -> Client: Merge 3 Responses
Client --> User: Display Unified Screen

@enduml
```

### 8.2 하루 기록 완료 플로우

```plantuml
@startuml 하루 기록 완료 플로우
actor User
participant Client
participant Server
database MySQL
database Redis

User -> Client: 저녁에 앱 접속

== 습관 체크 ==
loop 각 습관별
    User -> Client: 습관 체크
    Client -> Server: POST /api/habit-logs\n{userHabitId, checked: true}
    Server -> MySQL: HabitLog 저장
    Server -> MySQL: 스트릭 업데이트
    Server -> MySQL: 뱃지 진행 업데이트
    Server -> Redis: 캐시 갱신
    Server --> Client: 체크 완료

    alt 뱃지 달성
        Client --> User: 🎉 뱃지 획득 알림!
    end
end

== 데일리 페이지 작성 ==
User -> Client: 오늘 페이지 작성
Client -> Server: POST /api/daily-pages\n{content}
Server -> MySQL: DailyPage 저장
Server --> Client: 저장 완료
Client --> User: 오늘 기록 완료!

== 다음날 아침 ==
User -> Client: 앱 접속
Client -> Server: GET /api/ai-feedback/today
Server -> MySQL: 어제 기록 기반 피드백 생성
Server --> Client: 피드백
Client --> User: "어제 3개 습관 모두 성공! 대단해요 💪"

@enduml
```

---

## 문서 정보

| 항목 | 내용 |
|------|------|
| 작성일 | 2025년 1월 27일 |
| 최종 수정일 | 2025년 2월 3일 |
| 작성자 | 정우찬 |
| 관련 문서 | ERD 설계문서, API 명세서 v2 |
