# H2 Database 설정 가이드

> 새 프로젝트에서 H2 DB 설정할 때 참고용

---

## 1. H2 연결 방식 3가지

| 방식 | JDBC URL 예시 | 특징 |
|------|---------------|------|
| **Embedded** | `jdbc:h2:~/test` 또는 `jdbc:h2:mem:testdb` | 애플리케이션 내부에서만 접근, 프로세스 종료 시 연결 끊김, 동시 접근 불가 |
| **Server (TCP)** | `jdbc:h2:tcp://localhost/~/test` | H2 서버 별도 실행 필요, **여러 클라이언트 동시 접근 가능** |
| **Mixed** | `jdbc:h2:~/test;AUTO_SERVER=TRUE` | Embedded + TCP 동시 지원 |

### 왜 TCP 모드를 쓰나?
- Spring Boot 앱 + IntelliJ + DBeaver 등 **여러 도구에서 동시에** 같은 DB 확인 가능
- 개발 중 데이터 확인이 편함

---

## 2. H2 TCP 연결 설정 (Quick Start)

### Step 1: H2 Server 실행
```bash
# H2 설치 폴더에서
cd h2/bin
./h2.sh      # Mac/Linux
h2.bat       # Windows
```

### Step 2: DB 생성 (최초 1회, H2 Console에서)
```
# Generic H2 (Embedded) 선택
jdbc:h2:~/프로젝트명/database/db파일명
```
→ Connect 클릭하면 `~/프로젝트명/database/` 폴더에 `.mv.db` 파일 생성됨

### Step 3: TCP 연결 (애플리케이션에서 사용)
```
jdbc:h2:tcp://localhost/~/프로젝트명/database/db파일명
```

---

## 3. Spring Boot 설정

### build.gradle
```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'com.h2database:h2'
}
```

### application.yml (TCP 연결)
```yaml
spring:
  datasource:
    url: jdbc:h2:tcp://localhost/~/프로젝트명/database/db파일명
    username: sa
    password:
    driver-class-name: org.h2.Driver

  jpa:
    hibernate:
      ddl-auto: update    # 개발: update, 운영: validate 또는 none
    show-sql: true
    database-platform: org.hibernate.dialect.H2Dialect
```

### application.properties (동일 설정)
```properties
spring.datasource.url=jdbc:h2:tcp://localhost/~/프로젝트명/database/db파일명
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

---

## 4. SQL 언어 분류

| 분류 | 이름 | 명령어 | 용도 |
|------|------|--------|------|
| **DDL** | Data Definition Language | `CREATE`, `ALTER`, `DROP`, `TRUNCATE` | 테이블 구조 정의/변경 |
| **DML** | Data Manipulation Language | `SELECT`, `INSERT`, `UPDATE`, `DELETE` | 데이터 조회/조작 |
| **DCL** | Data Control Language | `GRANT`, `REVOKE` | 권한 관리 |
| **TCL** | Transaction Control Language | `COMMIT`, `ROLLBACK`, `SAVEPOINT` | 트랜잭션 제어 |

---

## 5. ddl-auto 전략

| 옵션 | 동작 | 사용 시점 |
|------|------|----------|
| `create` | 시작 시 DROP → CREATE | 테스트 (데이터 매번 초기화) |
| `create-drop` | 시작 시 CREATE, 종료 시 DROP | 테스트 (완전 초기화) |
| `update` | 변경점만 자동 반영 (컬럼 추가 등) | **개발 단계 추천** |
| `validate` | 엔티티 ↔ 스키마 일치 검증만 | **운영 단계 추천** |
| `none` | 아무것도 안 함 | 직접 DDL 스크립트 관리 시 |

### ddl-auto=none 사용 시 (직접 스크립트 관리)
```yaml
spring:
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql   # 초기 데이터 (선택)
  jpa:
    hibernate:
      ddl-auto: none
```

**파일 위치:**
- `src/main/resources/schema.sql` - DDL (CREATE TABLE 등)
- `src/main/resources/data.sql` - 초기 데이터 (INSERT 등)

---

## 6. 새 프로젝트 설정 체크리스트

```
□ H2 다운로드 및 설치
□ H2 Server 실행 (h2.sh 또는 h2.bat)
□ H2 Console에서 DB 생성 (Embedded 모드로 최초 연결)
□ build.gradle에 의존성 추가
□ application.yml에 TCP URL 설정
□ ddl-auto: update로 개발 시작
□ 운영 배포 전 validate 또는 none으로 변경
```

---

## 7. 트러블슈팅

### "Database not found" 에러
→ H2 Console에서 Embedded 모드로 DB 먼저 생성했는지 확인

### "Connection refused" 에러
→ H2 Server가 실행 중인지 확인 (`./h2.sh`)

### 테이블이 안 보임
→ `ddl-auto` 설정 확인, Entity 클래스에 `@Entity` 어노테이션 확인

---

**작성일:** 2025-01-28
