# 01. 프로젝트 기초 설정

> 커밋: `chore: initialize project with dependencies and package structure`

---

## 작업 내용

### 1. build.gradle 의존성 추가

```groovy
dependencies {
    // Spring Boot Starters
    implementation 'spring-boot-starter-web'        // REST API
    implementation 'spring-boot-starter-data-jpa'   // JPA/Hibernate
    implementation 'spring-boot-starter-validation' // @Valid, @NotBlank 등
    implementation 'spring-boot-starter-security'   // 인증/인가
    implementation 'spring-boot-starter-data-redis' // Redis 캐싱

    // Database
    runtimeOnly 'mysql-connector-j'    // MySQL (운영용)
    runtimeOnly 'h2'                   // H2 (로컬 개발용)

    // JWT
    implementation 'jjwt-api'          // JWT 생성/검증

    // Lombok
    compileOnly 'lombok'               // 보일러플레이트 제거

    // Swagger
    implementation 'springdoc-openapi-starter-webmvc-ui'  // API 문서화
}
```

### 2. application.yml 설정

- **Profile 분리**: local / dev / prod
- **H2 TCP 연결**: `jdbc:h2:tcp://localhost/~/dailyonepage/database/dailyonepage`
- **환경변수 사용**: `${DB_PASSWORD:기본값}` 형태로 민감정보 보호

### 3. 패키지 구조 (도메인 중심)

```
com.dailyonepage.backend
├── domain/                    # 도메인별 모듈
│   ├── user/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   └── dto/
│   ├── habit/
│   ├── dailypage/
│   ├── badge/
│   └── ai/
├── global/                    # 공통 모듈
│   ├── config/
│   ├── exception/
│   ├── security/
│   └── common/
└── infra/                     # 외부 인프라
    └── redis/
```

---

## 핵심 학습 포인트

### 1. Gradle 의존성 키워드

| 키워드 | 의미 | 예시 |
|--------|------|------|
| `implementation` | 컴파일 + 런타임에 필요 | spring-boot-starter-web |
| `runtimeOnly` | 런타임에만 필요 (코드에서 직접 사용 X) | mysql-connector-j, h2 |
| `compileOnly` | 컴파일에만 필요 | lombok |
| `annotationProcessor` | 컴파일 시 어노테이션 처리 | lombok |

**왜 DB 드라이버는 runtimeOnly?**
→ 코드에서는 JPA 인터페이스만 사용하고, 실제 DB 연결은 런타임에 드라이버가 처리

### 2. 도메인 중심 vs 계층 중심 패키지

**계층 중심 (비추천):**
```
├── controller/
│   ├── UserController
│   ├── HabitController
├── service/
├── repository/
```
→ 파일 많아지면 관련 코드 찾기 어려움

**도메인 중심 (추천):**
```
├── domain/user/
│   ├── controller/
│   ├── service/
├── domain/habit/
```
→ 관련 코드가 한 폴더에, MSA 분리 시 유리

### 3. JPA Auditing (BaseTimeEntity)

모든 테이블에 `created_at`, `updated_at` 자동 관리:

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

### 4. 전역 예외 처리

Controller마다 try-catch 반복 제거, 일관된 에러 응답:

```
[Controller] → 예외 발생 → [GlobalExceptionHandler] → ApiResponse.error()
```

---

**작성일:** 2025-01-28
