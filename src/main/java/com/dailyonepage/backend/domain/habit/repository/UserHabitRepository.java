package com.dailyonepage.backend.domain.habit.repository;

import com.dailyonepage.backend.domain.habit.entity.UserHabit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserHabitRepository extends JpaRepository<UserHabit, Long> {

    // 사용자의 습관 목록 (습관 정보와 함께)
    @Query("SELECT uh FROM UserHabit uh JOIN FETCH uh.habit WHERE uh.user.id = :userId")
    List<UserHabit> findByUserIdWithHabit(@Param("userId") Long userId);

    // 상세 조회 (습관 정보와 함께)
    @Query("SELECT uh FROM UserHabit uh JOIN FETCH uh.habit WHERE uh.id = :id")
    Optional<UserHabit> findByIdWithHabit(@Param("id") Long id);

    // 사용자의 특정 습관 조회
    Optional<UserHabit> findByUserIdAndHabitId(Long userId, Long habitId);

    // 중복 체크
    boolean existsByUserIdAndHabitId(Long userId, Long habitId);

    // 사용자의 습관 개수
    long countByUserId(Long userId);
}
