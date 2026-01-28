package com.dailyonepage.backend.domain.habit.repository;

import com.dailyonepage.backend.domain.habit.entity.HabitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HabitLogRepository extends JpaRepository<HabitLog, Long> {

    // 특정 날짜의 습관 로그 조회
    Optional<HabitLog> findByUserHabitIdAndDate(Long userHabitId, LocalDate date);

    // 기간 내 습관 로그 조회
    @Query("SELECT hl FROM HabitLog hl WHERE hl.userHabit.id = :userHabitId " +
            "AND hl.date BETWEEN :startDate AND :endDate ORDER BY hl.date DESC")
    List<HabitLog> findByUserHabitIdAndDateBetween(
            @Param("userHabitId") Long userHabitId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 사용자의 특정 날짜 모든 습관 로그
    @Query("SELECT hl FROM HabitLog hl " +
            "JOIN FETCH hl.userHabit uh " +
            "JOIN FETCH uh.habit " +
            "WHERE uh.user.id = :userId AND hl.date = :date")
    List<HabitLog> findByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    // 연속 체크 일수 계산을 위한 로그 조회 (최근순)
    @Query("SELECT hl FROM HabitLog hl WHERE hl.userHabit.id = :userHabitId " +
            "AND hl.checked = true ORDER BY hl.date DESC")
    List<HabitLog> findCheckedLogsByUserHabitIdOrderByDateDesc(@Param("userHabitId") Long userHabitId);
}
