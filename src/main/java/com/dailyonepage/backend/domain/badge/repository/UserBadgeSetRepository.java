package com.dailyonepage.backend.domain.badge.repository;

import com.dailyonepage.backend.domain.badge.entity.UserBadgeSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserBadgeSetRepository extends JpaRepository<UserBadgeSet, Long> {

    // 사용자의 특정 습관에 대한 뱃지세트 진행 상황
    List<UserBadgeSet> findByUserIdAndUserHabitId(Long userId, Long userHabitId);

    // 중복 체크
    boolean existsByUserIdAndUserHabitIdAndBadgeSetId(Long userId, Long userHabitId, Long badgeSetId);

    // 사용자의 모든 진행 중인 뱃지세트 (뱃지 정보와 함께)
    @Query("SELECT ubs FROM UserBadgeSet ubs " +
            "JOIN FETCH ubs.currentBadge " +
            "JOIN FETCH ubs.badgeSet " +
            "WHERE ubs.user.id = :userId")
    List<UserBadgeSet> findByUserIdWithBadgeInfo(@Param("userId") Long userId);
}
