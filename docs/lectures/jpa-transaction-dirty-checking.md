# JPA Transaction과 Dirty Checking

## 개요

JPA의 트랜잭션과 Dirty Checking은 데이터베이스 작업의 일관성과 효율성을 보장하는 핵심 메커니즘이다. 이 문서에서는 Spring에서의 트랜잭션 관리와 JPA의 Dirty Checking 동작 원리를 설명한다.

## 1. Transaction 기본 개념

### 1.1 트랜잭션이란?

트랜잭션은 데이터베이스 작업의 논리적 단위로, ACID 속성을 보장한다.

| 속성 | 설명 | 예시 |
|------|------|------|
| Atomicity (원자성) | 모든 작업이 성공하거나 모두 실패 | 송금 시 출금과 입금이 모두 완료되거나 모두 취소 |
| Consistency (일관성) | 트랜잭션 전후로 데이터 무결성 유지 | 계좌 잔액은 음수가 될 수 없음 |
| Isolation (격리성) | 동시 트랜잭션 간 독립성 보장 | 다른 트랜잭션의 중간 상태를 볼 수 없음 |
| Durability (지속성) | 완료된 트랜잭션은 영구 보존 | 커밋 후 시스템 장애가 나도 데이터 유지 |

### 1.2 Spring의 @Transactional

```java
@Service
@RequiredArgsConstructor
public class HabitLogService {

    private final HabitLogRepository habitLogRepository;

    @Transactional
    public HabitLogResponse checkHabit(Long userId, HabitLogCreateRequest request) {
        // 이 메서드 내의 모든 DB 작업은 하나의 트랜잭션으로 처리
        // 예외 발생 시 자동으로 롤백

        HabitLog habitLog = HabitLog.builder()
                .userHabit(userHabit)
                .logDate(request.getDate())
                .checked(request.isChecked())
                .build();

        habitLogRepository.save(habitLog);

        // 스트릭 계산
        int currentStreak = calculateStreak(userHabit, request.getDate());

        return HabitLogResponse.from(habitLog, currentStreak);
    }
}
```

## 2. @Transactional 속성

### 2.1 readOnly

읽기 전용 트랜잭션을 설정한다. 성능 최적화에 도움이 된다.

```java
@Service
public class BadgeService {

    // 조회만 하는 메서드는 readOnly = true
    @Transactional(readOnly = true)
    public List<BadgeSetResponse> getAllBadgeSets() {
        return badgeSetRepository.findAllWithBadges().stream()
                .map(BadgeSetResponse::from)
                .collect(Collectors.toList());
    }

    // 데이터 변경이 있는 메서드는 기본값 사용
    @Transactional
    public void awardBadge(Long userId, Long badgeId) {
        // ...
    }
}
```

**readOnly = true의 효과:**
- Hibernate의 Dirty Checking 비활성화 → 스냅샷 저장 안 함
- 플러시 모드가 MANUAL로 설정 → 불필요한 flush 방지
- 일부 DB에서 읽기 전용 최적화 적용

### 2.2 propagation (전파 속성)

트랜잭션이 이미 존재할 때 어떻게 동작할지 결정한다.

```java
@Service
public class UserHabitService {

    // 기본값: 기존 트랜잭션이 있으면 참여, 없으면 새로 생성
    @Transactional(propagation = Propagation.REQUIRED)
    public void registerHabit(Long userId, Long habitId) {
        // ...
    }

    // 항상 새로운 트랜잭션 생성
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logActivity(Long userId, String action) {
        // 메인 트랜잭션이 롤백되어도 이 로그는 저장됨
    }

    // 트랜잭션 없이 실행 (기존 트랜잭션 일시 중단)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void heavyReadOperation() {
        // 긴 조회 작업 - 트랜잭션 없이 실행
    }
}
```

| 전파 속성 | 설명 |
|----------|------|
| REQUIRED (기본) | 기존 트랜잭션 참여, 없으면 새로 생성 |
| REQUIRES_NEW | 항상 새 트랜잭션 생성 (기존 것 일시 중단) |
| SUPPORTS | 기존 트랜잭션 있으면 참여, 없으면 없이 실행 |
| NOT_SUPPORTED | 트랜잭션 없이 실행 (기존 것 일시 중단) |
| MANDATORY | 반드시 기존 트랜잭션 필요 (없으면 예외) |
| NEVER | 트랜잭션이 있으면 예외 |
| NESTED | 중첩 트랜잭션 생성 (Savepoint 사용) |

### 2.3 isolation (격리 수준)

동시에 실행되는 트랜잭션 간의 격리 수준을 설정한다.

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public void updateStreak(Long userHabitId) {
    // ...
}
```

| 격리 수준 | Dirty Read | Non-Repeatable Read | Phantom Read |
|----------|:----------:|:------------------:|:------------:|
| READ_UNCOMMITTED | O | O | O |
| READ_COMMITTED | X | O | O |
| REPEATABLE_READ | X | X | O |
| SERIALIZABLE | X | X | X |

### 2.4 rollbackFor / noRollbackFor

롤백 조건을 지정한다.

```java
@Service
public class DailyPageService {

    // 모든 예외에서 롤백
    @Transactional(rollbackFor = Exception.class)
    public void savePage(DailyPageCreateRequest request) {
        // checked exception도 롤백
    }

    // 특정 예외는 롤백하지 않음
    @Transactional(noRollbackFor = BusinessException.class)
    public void processWithRetry() {
        // BusinessException 발생해도 롤백 안 함
    }
}
```

**기본 롤백 규칙:**
- RuntimeException과 Error → 롤백
- Checked Exception → 롤백 안 함 (커밋)

## 3. Dirty Checking (변경 감지)

### 3.1 Dirty Checking이란?

JPA는 영속성 컨텍스트에서 엔티티의 변경을 자동으로 감지하여 UPDATE 쿼리를 생성한다.

```java
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public void updateNickname(Long userId, String newNickname) {
        // 1. 엔티티 조회 (영속 상태)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. 엔티티 수정 (Dirty 상태)
        user.updateNickname(newNickname);

        // 3. save() 호출 필요 없음!
        // 트랜잭션 종료 시 자동으로 UPDATE 쿼리 실행
    }
}
```

### 3.2 동작 원리

```
1. 엔티티 조회 시
   ┌─────────────────────────────────────────────┐
   │ 영속성 컨텍스트                              │
   │                                             │
   │  1차 캐시                스냅샷 저장소        │
   │  ┌────────────┐         ┌────────────┐      │
   │  │ @Id: 1     │         │ @Id: 1     │      │
   │  │ name: "홍길동"│  ←복사→ │ name: "홍길동"│   │
   │  │ email: ... │         │ email: ... │      │
   │  └────────────┘         └────────────┘      │
   │                                             │
   └─────────────────────────────────────────────┘

2. 엔티티 수정 시
   ┌─────────────────────────────────────────────┐
   │ 영속성 컨텍스트                              │
   │                                             │
   │  1차 캐시 (변경됨)       스냅샷 (원본 유지)    │
   │  ┌────────────┐         ┌────────────┐      │
   │  │ @Id: 1     │         │ @Id: 1     │      │
   │  │ name: "김철수"│   ≠    │ name: "홍길동"│   │
   │  │ email: ... │         │ email: ... │      │
   │  └────────────┘         └────────────┘      │
   │       ↓                                     │
   │   Dirty!                                    │
   └─────────────────────────────────────────────┘

3. 트랜잭션 커밋 시 (flush)
   - 1차 캐시와 스냅샷 비교
   - 변경된 필드에 대해 UPDATE SQL 생성
   - 쿼리 실행
```

### 3.3 오늘한장 프로젝트 예시

```java
// DailyPageService.java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyPageService {

    private final DailyPageRepository dailyPageRepository;

    @Transactional  // readOnly = false로 오버라이드
    public DailyPageResponse updatePage(Long userId, Long pageId,
                                        DailyPageUpdateRequest request) {
        // 1. 조회 - 영속 상태로 관리됨
        DailyPage page = dailyPageRepository.findById(pageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAGE_NOT_FOUND));

        // 2. 권한 확인
        if (!page.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 3. 변경 - Dirty Checking 발생
        page.updateContent(request.getContent());

        // 4. save() 호출 없이 트랜잭션 종료 시 자동 UPDATE
        return DailyPageResponse.from(page);
    }
}

// DailyPage.java (엔티티)
@Entity
public class DailyPage extends BaseTimeEntity {

    // ... 필드들

    // 변경 메서드는 엔티티 내부에 정의
    public void updateContent(String content) {
        this.content = content;
        // updatedAt은 @LastModifiedDate로 자동 갱신
    }
}
```

### 3.4 Dirty Checking 주의사항

#### 영속 상태가 아니면 동작하지 않음

```java
// ❌ 잘못된 예시 - 준영속 상태
@Transactional
public void updateWrong(UserUpdateRequest request) {
    User user = new User();  // 비영속 상태
    user.setId(request.getId());
    user.setNickname(request.getNickname());
    // Dirty Checking 동작 안 함!
}

// ✅ 올바른 예시 - 영속 상태
@Transactional
public void updateCorrect(Long userId, UserUpdateRequest request) {
    User user = userRepository.findById(userId)  // 영속 상태
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    user.updateNickname(request.getNickname());
    // Dirty Checking 동작함
}
```

#### readOnly = true 에서는 동작하지 않음

```java
@Transactional(readOnly = true)
public void tryUpdate(Long userId, String nickname) {
    User user = userRepository.findById(userId).orElseThrow();
    user.updateNickname(nickname);
    // ⚠️ readOnly = true이므로 UPDATE 쿼리 실행 안 됨!
}
```

## 4. 플러시(Flush)

### 4.1 플러시란?

영속성 컨텍스트의 변경 내용을 데이터베이스에 동기화하는 작업이다.

```java
@Transactional
public void example() {
    User user = userRepository.findById(1L).orElseThrow();
    user.updateNickname("새이름");

    // 플러시 발생 시점:
    // 1. 트랜잭션 커밋 시 (자동)
    // 2. JPQL 쿼리 실행 전 (자동)
    // 3. entityManager.flush() 호출 시 (수동)

    // JPQL 실행 전 자동 플러시 발생
    List<User> users = userRepository.findByNicknameContaining("새");
    // 위 쿼리 결과에 방금 변경한 user가 포함됨
}
```

### 4.2 플러시 모드

```java
// application.yml
spring:
  jpa:
    properties:
      hibernate:
        flushMode: AUTO  # 기본값

# AUTO: JPQL 실행 전, 커밋 전에 자동 플러시
# COMMIT: 커밋 시에만 플러시
# MANUAL: 수동으로만 플러시
```

## 5. 트랜잭션 전파와 Dirty Checking 실전 예제

### 5.1 습관 체크 후 뱃지 확인

```java
@Service
@RequiredArgsConstructor
public class HabitLogService {

    private final HabitLogRepository habitLogRepository;
    private final BadgeService badgeService;

    @Transactional
    public HabitLogResponse checkHabit(Long userId, HabitLogCreateRequest request) {
        // 1. 습관 로그 저장
        HabitLog log = createHabitLog(request);
        habitLogRepository.save(log);

        // 2. 스트릭 업데이트 (Dirty Checking)
        UserHabit userHabit = log.getUserHabit();
        userHabit.updateStreak(calculateStreak(userHabit, request.getDate()));

        // 3. 뱃지 확인 (같은 트랜잭션에서 실행)
        badgeService.checkAndAwardBadges(userId, userHabit.getCurrentStreak());

        return HabitLogResponse.from(log, userHabit.getCurrentStreak());
    }
}

@Service
public class BadgeService {

    // REQUIRED이므로 기존 트랜잭션에 참여
    @Transactional
    public void checkAndAwardBadges(Long userId, int streak) {
        // 스트릭 기반 뱃지 확인 및 부여
        if (streak >= 7) {
            awardBadgeIfNotExists(userId, "STREAK_7");
        }
        // ...
    }
}
```

### 5.2 독립적인 로그 저장 (REQUIRES_NEW)

```java
@Service
public class ActivityLogService {

    // 새로운 트랜잭션에서 실행 - 메인 트랜잭션 롤백되어도 저장됨
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logActivity(Long userId, String action, String detail) {
        ActivityLog log = ActivityLog.builder()
                .userId(userId)
                .action(action)
                .detail(detail)
                .createdAt(LocalDateTime.now())
                .build();

        activityLogRepository.save(log);
    }
}

@Service
public class DailyPageService {

    private final ActivityLogService activityLogService;

    @Transactional
    public DailyPageResponse savePage(Long userId, DailyPageCreateRequest request) {
        try {
            DailyPage page = createPage(userId, request);
            dailyPageRepository.save(page);

            // 로그는 별도 트랜잭션
            activityLogService.logActivity(userId, "PAGE_CREATE", page.getId().toString());

            return DailyPageResponse.from(page);
        } catch (Exception e) {
            // 페이지 저장 실패해도 활동 로그는 저장됨
            throw e;
        }
    }
}
```

## 6. 베스트 프랙티스

### 6.1 Service 클래스 레벨 @Transactional

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 클래스 레벨: 읽기 전용
public class UserHabitService {

    // 조회 메서드는 클래스 레벨 설정 사용
    public UserHabitListResponse getMyHabits(Long userId) {
        // readOnly = true
    }

    // 변경 메서드는 오버라이드
    @Transactional  // readOnly = false
    public UserHabitResponse registerHabit(Long userId, UserHabitCreateRequest request) {
        // 쓰기 가능
    }
}
```

### 6.2 엔티티 변경 메서드 캡슐화

```java
// ✅ 좋은 예시 - 변경 메서드를 엔티티에 정의
@Entity
public class User {

    private String nickname;
    private String email;

    // 비즈니스 의미가 있는 변경 메서드
    public void updateProfile(String nickname, String email) {
        validateNickname(nickname);
        validateEmail(email);
        this.nickname = nickname;
        this.email = email;
    }

    private void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("닉네임은 필수입니다.");
        }
    }
}

// Service에서 사용
@Transactional
public void updateProfile(Long userId, ProfileUpdateRequest request) {
    User user = userRepository.findById(userId).orElseThrow();
    user.updateProfile(request.getNickname(), request.getEmail());
    // Dirty Checking으로 자동 UPDATE
}
```

### 6.3 주의: @Transactional과 프록시

```java
@Service
public class SomeService {

    @Transactional
    public void methodA() {
        // 트랜잭션 시작
        this.methodB();  // ⚠️ 같은 클래스 내부 호출 - 프록시 우회!
        // methodB의 @Transactional 무시됨
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void methodB() {
        // 새 트랜잭션이 시작되어야 하지만...
        // 내부 호출이라 실제로는 methodA의 트랜잭션 사용
    }
}

// ✅ 해결책: 별도 빈으로 분리
@Service
public class ServiceA {
    private final ServiceB serviceB;  // 다른 빈 주입

    @Transactional
    public void methodA() {
        serviceB.methodB();  // 프록시를 통해 호출 - 정상 동작
    }
}
```

## 요약

| 개념 | 핵심 포인트 |
|------|------------|
| @Transactional | 메서드/클래스에 적용, 예외 시 자동 롤백 |
| readOnly | 조회 메서드에 적용하여 성능 최적화 |
| propagation | 트랜잭션 전파 방식 제어 (REQUIRED, REQUIRES_NEW 등) |
| Dirty Checking | 영속 상태 엔티티의 변경을 자동 감지하여 UPDATE |
| Flush | 영속성 컨텍스트와 DB 동기화 |
| 프록시 | 같은 클래스 내부 호출 시 @Transactional 무시됨 |
