package com.dailyonepage.backend.domain.user.repository;

import com.dailyonepage.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * User Repository
 *
 * JpaRepository를 상속받으면 기본 CRUD 메서드 자동 제공:
 * - save(), findById(), findAll(), delete(), count() 등
 *
 * 메서드 이름만으로 쿼리 자동 생성 (Query Method):
 * - findByEmail() → SELECT * FROM users WHERE email = ?
 * - existsByEmail() → SELECT COUNT(*) > 0 FROM users WHERE email = ?
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);
}
