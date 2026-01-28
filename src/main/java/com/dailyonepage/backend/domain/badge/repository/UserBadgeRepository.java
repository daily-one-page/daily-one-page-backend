package com.dailyonepage.backend.domain.badge.repository;

import com.dailyonepage.backend.domain.badge.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    // 사용자가 획득한 모든 뱃지
    @Query("SELECT ub FROM UserBadge ub " +
            "JOIN FETCH ub.badge b " +
            "JOIN FETCH b.badgeSet " +
            "WHERE ub.user.id = :userId " +
            "ORDER BY ub.completedAt DESC")
    List<UserBadge> findByUserIdWithBadgeInfo(@Param("userId") Long userId);

    // 사용자의 최근 획득 뱃지
    @Query("SELECT ub FROM UserBadge ub " +
            "JOIN FETCH ub.badge " +
            "WHERE ub.user.id = :userId " +
            "ORDER BY ub.completedAt DESC " +
            "LIMIT :limit")
    List<UserBadge> findRecentBadges(@Param("userId") Long userId, @Param("limit") int limit);

    // 획득 뱃지 개수
    long countByUserId(Long userId);
}
