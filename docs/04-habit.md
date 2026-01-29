# 04. Habit API (습관 관리)

> 커밋: `feat: implement habit API for system and custom habits`

---

## 작업 내용

| API | Method | URL | 설명 |
|-----|--------|-----|------|
| 시스템 습관 목록 | GET | `/api/habits?type=system` | 시스템 습관 + 뱃지세트 미리보기 |
| 커스텀 습관 생성 | POST | `/api/habits` | 사용자 정의 습관 생성 |
| 커스텀 습관 수정 | PUT | `/api/habits/{id}` | 커스텀 습관 수정 (본인만) |
| 커스텀 습관 삭제 | DELETE | `/api/habits/{id}` | 커스텀 습관 삭제 (본인만) |

---

## 핵심 학습 포인트

### 1. 시스템 습관 vs 커스텀 습관

```
Habit 테이블
┌────┬─────────┬──────────┬────────────┐
│ id │ user_id │  name    │    type    │
├────┼─────────┼──────────┼────────────┤
│ 1  │  null   │ 달리기    │ PRACTICE   │  ← 시스템 습관 (모든 사용자 공용)
│ 2  │  null   │ 금연     │ ABSTINENCE │  ← 시스템 습관
│ 3  │   1     │ 명상하기  │ PRACTICE   │  ← user_id=1의 커스텀 습관
└────┴─────────┴──────────┴────────────┘
```

**구분 기준:**
- `user_id = null` → 시스템 습관 (관리자가 미리 등록)
- `user_id = 값` → 해당 사용자의 커스텀 습관

**왜 이렇게 설계?**
- 시스템 습관: 모든 사용자가 선택 가능, 뱃지세트 연동
- 커스텀 습관: 사용자가 자유롭게 만들 수 있음

### 2. 습관 타입 (HabitType)

```java
public enum HabitType {
    PRACTICE,    // 실천형: 달리기, 독서, 명상 등 (하면 체크)
    ABSTINENCE   // 금지형: 금연, 금주 등 (안 하면 체크)
}
```

**타입별 특성:**

| 타입 | 의미 | 체크 기준 | 예시 |
|------|------|---------|------|
| PRACTICE | 실천형 | "오늘 했다" | 달리기, 독서, 운동 |
| ABSTINENCE | 금지형 | "오늘 안 했다" | 금연, 금주, 야식 금지 |

### 3. 엔티티 수정 메서드 패턴

```java
@Entity
public class Habit {
    // ...

    // 수정 메서드 - Setter 대신 의미 있는 메서드명
    public void update(String name, HabitType type) {
        this.name = name;
        this.type = type;
    }

    // 소유자 확인 메서드
    public boolean isOwnedBy(Long userId) {
        return this.user != null && this.user.getId().equals(userId);
    }
}
```

**Setter를 쓰지 않는 이유:**
- `setName()`: 단순 값 변경 (의도 불명확)
- `update()`: 습관 정보 수정이라는 비즈니스 의도 명확
- 도메인 객체가 자신의 상태를 스스로 관리

### 4. SecurityUtil - 현재 사용자 조회

```java
public class SecurityUtil {
    public static String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() ||
                auth.getPrincipal().equals("anonymousUser")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return auth.getName();  // JWT에서 추출한 이메일
    }
}
```

**사용 흐름:**
```
요청 → JwtAuthenticationFilter (토큰 검증)
     → SecurityContext에 Authentication 저장
     → Controller에서 SecurityUtil.getCurrentUserEmail()로 조회
```

### 5. 권한 검증 패턴

```java
@Service
public class HabitService {

    public void updateCustomHabit(Long userId, Long habitId, ...) {
        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new BusinessException(ErrorCode.HABIT_NOT_FOUND));

        // 1. 시스템 습관 수정 불가
        if (habit.isSystemHabit()) {
            throw new BusinessException(ErrorCode.SYSTEM_HABIT_NOT_MODIFIABLE);
        }

        // 2. 본인 소유 확인
        if (!habit.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.HABIT_NOT_OWNED);
        }

        // 3. 수정 실행
        habit.update(...);
    }
}
```

**검증 순서:**
1. 리소스 존재 확인 (404)
2. 리소스 유형 확인 (400 - 시스템 습관)
3. 소유권 확인 (403)
4. 비즈니스 로직 실행

### 6. @Transactional과 더티 체킹

```java
@Service
@Transactional(readOnly = true)  // 기본: 읽기 전용
public class HabitService {

    @Transactional  // 쓰기 필요한 메서드
    public HabitResponse updateCustomHabit(...) {
        Habit habit = habitRepository.findById(habitId).orElseThrow(...);
        habit.update(name, type);  // 엔티티 수정
        // save() 호출 안 해도 됨! → 더티 체킹으로 자동 UPDATE
        return HabitResponse.from(habit);
    }
}
```

**더티 체킹(Dirty Checking):**
- 트랜잭션 내에서 영속 상태 엔티티의 변경 자동 감지
- 트랜잭션 커밋 시점에 UPDATE 쿼리 자동 실행
- `repository.save()` 명시적 호출 불필요

### 7. 연관 데이터 Cascade 삭제

```java
@Entity
public class Habit {
    // Habit 삭제 시 연관된 UserHabit, HabitLog 등도 삭제
    @OneToMany(mappedBy = "habit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserHabit> userHabits = new ArrayList<>();
}
```

**주의사항:**
- 커스텀 습관 삭제 시 해당 습관의 모든 기록도 삭제됨
- 사용자에게 경고 필요 (프론트엔드에서 확인 대화상자)

---

## 파일 구조

```
domain/habit/
├── controller/
│   └── HabitController.java
├── service/
│   └── HabitService.java
├── dto/
│   ├── HabitCreateRequest.java
│   ├── HabitUpdateRequest.java
│   ├── HabitResponse.java
│   ├── SystemHabitResponse.java
│   ├── SystemHabitListResponse.java
│   └── BadgeSetSimpleResponse.java
├── entity/
│   ├── Habit.java (수정됨)
│   └── HabitType.java
└── repository/
    └── HabitRepository.java

global/security/
└── SecurityUtil.java (추가됨)

global/exception/
└── ErrorCode.java (에러 코드 추가)
```

---

## API 테스트

### 1. 로그인 (토큰 발급)
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"password123"}'

# accessToken 복사
```

### 2. 시스템 습관 조회
```bash
curl http://localhost:8080/api/habits?type=system \
  -H "Authorization: Bearer {accessToken}"
```

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "habits": [
      {
        "id": 1,
        "name": "달리기",
        "type": "PRACTICE",
        "badgeSets": [
          {"id": 1, "name": "거리 도전", "description": "달린 거리로 뱃지 획득"}
        ]
      }
    ]
  }
}
```

### 3. 커스텀 습관 생성
```bash
curl -X POST http://localhost:8080/api/habits \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{"name":"명상하기","type":"PRACTICE"}'
```

### 4. 커스텀 습관 수정
```bash
curl -X PUT http://localhost:8080/api/habits/3 \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{"name":"아침 명상","type":"PRACTICE"}'
```

### 5. 커스텀 습관 삭제
```bash
curl -X DELETE http://localhost:8080/api/habits/3 \
  -H "Authorization: Bearer {accessToken}"
```

---

## 검증 방법

1. 앱 실행
2. Swagger UI: `http://localhost:8080/swagger-ui/index.html`
3. Habit API 테스트
   - 로그인 후 Access Token 획득
   - 시스템 습관 목록 조회 확인
   - 커스텀 습관 CRUD 테스트
   - 다른 사용자 습관 수정/삭제 시 403 에러 확인

---

**작성일:** 2025-01-29
