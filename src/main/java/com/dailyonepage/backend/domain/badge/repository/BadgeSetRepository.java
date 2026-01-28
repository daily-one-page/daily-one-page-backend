package com.dailyonepage.backend.domain.badge.repository;

import com.dailyonepage.backend.domain.badge.entity.BadgeSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BadgeSetRepository extends JpaRepository<BadgeSet, Long> {

    // 범용 뱃지세트 조회 (모든 습관에 적용)
    @Query("SELECT bs FROM BadgeSet bs WHERE bs.user IS NULL AND bs.habit IS NULL")
    List<BadgeSet> findUniversalBadgeSets();

    // 특정 습관의 시스템 뱃지세트 조회
    @Query("SELECT bs FROM BadgeSet bs WHERE bs.user IS NULL AND bs.habit.id = :habitId")
    List<BadgeSet> findSystemBadgeSetsByHabitId(@Param("habitId") Long habitId);

    // 습관에 적용될 모든 뱃지세트 (전용 + 범용)
    @Query("SELECT bs FROM BadgeSet bs WHERE bs.user IS NULL " +
            "AND (bs.habit.id = :habitId OR bs.habit IS NULL)")
    List<BadgeSet> findApplicableBadgeSets(@Param("habitId") Long habitId);
}
