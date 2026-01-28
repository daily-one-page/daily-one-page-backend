package com.dailyonepage.backend.domain.badge.repository;

import com.dailyonepage.backend.domain.badge.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BadgeRepository extends JpaRepository<Badge, Long> {

    // 뱃지세트의 첫 번째 뱃지 조회
    @Query("SELECT b FROM Badge b WHERE b.badgeSet.id = :badgeSetId ORDER BY b.sequence ASC LIMIT 1")
    Optional<Badge> findFirstByBadgeSetId(@Param("badgeSetId") Long badgeSetId);

    // 뱃지세트의 모든 뱃지 (순서대로)
    List<Badge> findByBadgeSetIdOrderBySequenceAsc(Long badgeSetId);

    // 현재 뱃지의 다음 뱃지 조회
    @Query("SELECT b FROM Badge b WHERE b.badgeSet.id = :badgeSetId " +
            "AND b.sequence > :currentSequence ORDER BY b.sequence ASC LIMIT 1")
    Optional<Badge> findNextBadge(@Param("badgeSetId") Long badgeSetId,
                                   @Param("currentSequence") int currentSequence);
}
