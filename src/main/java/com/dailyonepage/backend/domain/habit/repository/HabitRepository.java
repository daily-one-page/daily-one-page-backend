package com.dailyonepage.backend.domain.habit.repository;

import com.dailyonepage.backend.domain.habit.entity.Habit;
import com.dailyonepage.backend.domain.habit.entity.HabitType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HabitRepository extends JpaRepository<Habit, Long> {

    // 시스템 습관 목록 조회
    @Query("SELECT h FROM Habit h WHERE h.user IS NULL")
    List<Habit> findSystemHabits();

    // 시스템 습관 타입별 조회
    @Query("SELECT h FROM Habit h WHERE h.user IS NULL AND h.type = :type")
    List<Habit> findSystemHabitsByType(@Param("type") HabitType type);

    // 사용자의 커스텀 습관 목록
    List<Habit> findByUserId(Long userId);
}
