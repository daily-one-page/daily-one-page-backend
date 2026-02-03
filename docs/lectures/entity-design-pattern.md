# JPA Entity 설계 패턴

## 개요

JPA 엔티티를 효과적으로 설계하면 유지보수성, 안정성, 가독성이 크게 향상된다. 이 문서에서는 오늘한장 프로젝트에서 사용한 엔티티 설계 패턴들을 정리한다.

## 1. BaseTimeEntity 패턴

### 1.1 JPA Auditing

모든 엔티티에 생성일시, 수정일시를 자동으로 관리한다.

```java
// BaseTimeEntity.java
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

```java
// JpaConfig.java
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
```

### 1.2 엔티티에서 상속

```java
@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {
    // createdAt, updatedAt 자동 관리
}

@Entity
public class DailyPage extends BaseTimeEntity {
    // createdAt, updatedAt 자동 관리
}
```

### 1.3 사용 시점

| 어노테이션 | 설정 시점 | 변경 시점 |
|-----------|----------|----------|
| @CreatedDate | INSERT 시 | 이후 변경 불가 |
| @LastModifiedDate | INSERT 시 | 모든 UPDATE 시 갱신 |

## 2. 엔티티 생성 패턴

### 2.1 Builder 패턴

Lombok의 @Builder를 사용하여 가독성 높은 객체 생성을 제공한다.

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Habit extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String icon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;  // null이면 시스템 습관

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HabitType type;

    @Builder
    private Habit(String name, String description, String icon,
                  User user, HabitType type) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.user = user;
        this.type = type;
    }
}
```

**생성 코드:**

```java
// 시스템 습관
Habit systemHabit = Habit.builder()
        .name("물 마시기")
        .description("하루 8잔 물 마시기")
        .icon("💧")
        .type(HabitType.SYSTEM)
        .build();

// 커스텀 습관
Habit customHabit = Habit.builder()
        .name("독서하기")
        .description("30분 책 읽기")
        .icon("📚")
        .user(user)
        .type(HabitType.CUSTOM)
        .build();
```

### 2.2 정적 팩토리 메서드 패턴

복잡한 생성 로직이나 의미 있는 이름이 필요할 때 사용한다.

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserHabit extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;

    private int currentStreak;
    private int bestStreak;
    private LocalDate startedAt;

    // 정적 팩토리 메서드
    public static UserHabit register(User user, Habit habit) {
        UserHabit userHabit = new UserHabit();
        userHabit.user = user;
        userHabit.habit = habit;
        userHabit.currentStreak = 0;
        userHabit.bestStreak = 0;
        userHabit.startedAt = LocalDate.now();
        return userHabit;
    }
}
```

**사용 코드:**

```java
// 의미가 명확한 메서드명
UserHabit userHabit = UserHabit.register(user, habit);
```

### 2.3 @NoArgsConstructor(access = PROTECTED)

JPA는 기본 생성자가 필요하지만, 외부에서 직접 호출을 막는다.

```java
// ❌ public 기본 생성자 - 불완전한 객체 생성 가능
User user = new User();  // 모든 필드가 null

// ✅ protected 기본 생성자 + Builder/정적 팩토리
// 같은 패키지나 상속 클래스 외에는 접근 불가
// JPA 프록시 생성은 가능
```

## 3. 연관관계 설정 패턴

### 3.1 양방향 연관관계 편의 메서드

양방향 연관관계에서 일관성을 유지한다.

```java
@Entity
public class User extends BaseTimeEntity {

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserHabit> userHabits = new ArrayList<>();

    // 연관관계 편의 메서드
    public void addUserHabit(UserHabit userHabit) {
        this.userHabits.add(userHabit);
        userHabit.setUser(this);
    }

    public void removeUserHabit(UserHabit userHabit) {
        this.userHabits.remove(userHabit);
        userHabit.setUser(null);
    }
}

@Entity
public class UserHabit extends BaseTimeEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 패키지 레벨 setter (User 클래스에서만 호출)
    void setUser(User user) {
        this.user = user;
    }
}
```

### 3.2 지연 로딩 기본 설정

```java
@Entity
public class HabitLog extends BaseTimeEntity {

    // ✅ 항상 LAZY 로딩 사용
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_habit_id", nullable = false)
    private UserHabit userHabit;

    // ❌ EAGER 로딩 - 불필요한 조인 발생
    // @ManyToOne(fetch = FetchType.EAGER)
}
```

**연관관계 기본 설정:**

| 관계 | 기본 FetchType | 권장 설정 |
|------|---------------|----------|
| @ManyToOne | EAGER | LAZY |
| @OneToOne | EAGER | LAZY |
| @OneToMany | LAZY | LAZY |
| @ManyToMany | LAZY | LAZY |

### 3.3 Cascade 설정

```java
@Entity
public class BadgeSet extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // 부모 저장 시 자식도 함께 저장
    @OneToMany(mappedBy = "badgeSet", cascade = CascadeType.ALL,
               orphanRemoval = true)
    private List<Badge> badges = new ArrayList<>();

    public void addBadge(Badge badge) {
        this.badges.add(badge);
        badge.setBadgeSet(this);
    }
}
```

| Cascade 타입 | 설명 |
|-------------|------|
| PERSIST | 저장 시 함께 저장 |
| MERGE | 병합 시 함께 병합 |
| REMOVE | 삭제 시 함께 삭제 |
| ALL | 모든 동작 전파 |
| orphanRemoval | 부모와 연관 끊기면 삭제 |

## 4. 엔티티 변경 메서드 패턴

### 4.1 Setter 대신 의미 있는 메서드

```java
@Entity
public class DailyPage extends BaseTimeEntity {

    @Column(columnDefinition = "TEXT")
    private String content;

    // ❌ Setter 사용 금지
    // public void setContent(String content) {
    //     this.content = content;
    // }

    // ✅ 의미 있는 변경 메서드
    public void updateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("내용은 비어있을 수 없습니다.");
        }
        this.content = content;
    }
}
```

### 4.2 상태 변경 메서드

```java
@Entity
public class UserHabit extends BaseTimeEntity {

    private int currentStreak;
    private int bestStreak;

    @Enumerated(EnumType.STRING)
    private UserHabitStatus status;

    // 스트릭 업데이트
    public void updateStreak(int newStreak) {
        this.currentStreak = newStreak;
        if (newStreak > this.bestStreak) {
            this.bestStreak = newStreak;
        }
    }

    // 스트릭 리셋
    public void resetStreak() {
        this.currentStreak = 0;
    }

    // 상태 변경
    public void activate() {
        this.status = UserHabitStatus.ACTIVE;
    }

    public void pause() {
        this.status = UserHabitStatus.PAUSED;
    }

    public void complete() {
        this.status = UserHabitStatus.COMPLETED;
    }
}
```

### 4.3 불변 필드 보호

```java
@Entity
public class User extends BaseTimeEntity {

    @Column(nullable = false, unique = true)
    private String email;  // 변경 불가

    @Column(nullable = false)
    private String nickname;  // 변경 가능

    // email은 변경 메서드 제공하지 않음

    // nickname만 변경 가능
    public void updateNickname(String nickname) {
        validateNickname(nickname);
        this.nickname = nickname;
    }

    private void validateNickname(String nickname) {
        if (nickname == null || nickname.length() < 2) {
            throw new IllegalArgumentException("닉네임은 2자 이상이어야 합니다.");
        }
    }
}
```

## 5. Enum 활용 패턴

### 5.1 EnumType.STRING 사용

```java
@Entity
public class Habit extends BaseTimeEntity {

    // ✅ STRING 타입 - 가독성 좋음, 순서 변경 안전
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HabitType type;

    // ❌ ORDINAL 타입 - 순서 변경 시 데이터 깨짐
    // @Enumerated(EnumType.ORDINAL)
}
```

### 5.2 Enum에 비즈니스 로직 포함

```java
public enum BadgeGrade {
    BRONZE("동", 1),
    SILVER("은", 2),
    GOLD("금", 3),
    PLATINUM("플래티넘", 4);

    private final String displayName;
    private final int level;

    BadgeGrade(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isHigherThan(BadgeGrade other) {
        return this.level > other.level;
    }
}
```

## 6. ID 전략

### 6.1 IDENTITY 전략 (권장)

```java
@Entity
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
```

**전략 비교:**

| 전략 | 설명 | 특징 |
|------|------|------|
| IDENTITY | DB AUTO_INCREMENT | 즉시 INSERT 필요, 배치 INSERT 불가 |
| SEQUENCE | DB 시퀀스 사용 | 성능 좋음, 일부 DB만 지원 |
| TABLE | 키 전용 테이블 | 모든 DB 지원, 성능 낮음 |
| AUTO | DB에 따라 자동 선택 | 예측 어려움 |

### 6.2 복합키 패턴

```java
// 복합키 클래스
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class UserBadgeId implements Serializable {
    private Long userId;
    private Long badgeId;
}

// 엔티티
@Entity
public class UserBadge extends BaseTimeEntity {

    @EmbeddedId
    private UserBadgeId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("badgeId")
    @JoinColumn(name = "badge_id")
    private Badge badge;

    private LocalDate acquiredAt;
}
```

## 7. 컬럼 매핑 패턴

### 7.1 필수/선택 필드

```java
@Entity
public class DailyPage extends BaseTimeEntity {

    @Column(nullable = false)  // NOT NULL
    private LocalDate pageDate;

    @Column(columnDefinition = "TEXT")  // 긴 텍스트
    private String content;

    private String mood;  // nullable = true (기본값)
}
```

### 7.2 유니크 제약조건

```java
@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"email"})
})
public class User extends BaseTimeEntity {

    @Column(nullable = false, unique = true)
    private String email;
}

@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "habit_id"})
})
public class UserHabit extends BaseTimeEntity {
    // user_id + habit_id 조합이 유니크
}
```

### 7.3 인덱스 설정

```java
@Entity
@Table(name = "habit_logs", indexes = {
    @Index(name = "idx_habit_log_date", columnList = "user_habit_id, log_date"),
    @Index(name = "idx_habit_log_user_date", columnList = "user_habit_id, log_date DESC")
})
public class HabitLog extends BaseTimeEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_habit_id", nullable = false)
    private UserHabit userHabit;

    @Column(nullable = false)
    private LocalDate logDate;
}
```

## 8. 검증 패턴

### 8.1 엔티티 내부 검증

```java
@Entity
public class Habit extends BaseTimeEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @Builder
    private Habit(String name, String description, String icon,
                  User user, HabitType type) {
        validateName(name);
        validateIcon(icon);

        this.name = name;
        this.description = description;
        this.icon = icon;
        this.user = user;
        this.type = type;
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("습관 이름은 필수입니다.");
        }
        if (name.length() > 50) {
            throw new IllegalArgumentException("습관 이름은 50자 이하여야 합니다.");
        }
    }

    private void validateIcon(String icon) {
        if (icon == null || icon.isBlank()) {
            throw new IllegalArgumentException("아이콘은 필수입니다.");
        }
    }
}
```

### 8.2 Bean Validation과 함께 사용

```java
@Entity
public class User extends BaseTimeEntity {

    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @Size(min = 2, max = 20)
    @Column(nullable = false)
    private String nickname;

    // 생성 시 추가 비즈니스 검증
    @Builder
    private User(String email, String nickname, String password) {
        validateEmail(email);
        this.email = email;
        this.nickname = nickname;
        this.password = password;
    }

    private void validateEmail(String email) {
        if (email != null && email.contains("+")) {
            throw new IllegalArgumentException("+ 기호가 포함된 이메일은 사용할 수 없습니다.");
        }
    }
}
```

## 체크리스트

엔티티 설계 시 확인할 사항:

- [ ] BaseTimeEntity 상속
- [ ] @NoArgsConstructor(access = PROTECTED) 적용
- [ ] @Getter만 사용 (Setter 금지)
- [ ] Builder 또는 정적 팩토리 메서드 제공
- [ ] 모든 @ManyToOne, @OneToOne에 fetch = LAZY
- [ ] @Enumerated(EnumType.STRING) 사용
- [ ] 의미 있는 변경 메서드 제공
- [ ] 필수 필드에 @Column(nullable = false)
- [ ] 유니크 제약조건 설정
- [ ] 자주 조회되는 컬럼에 인덱스 설정
- [ ] 복잡한 검증 로직은 엔티티 내부에 구현
