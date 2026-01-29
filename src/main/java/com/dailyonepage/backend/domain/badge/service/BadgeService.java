package com.dailyonepage.backend.domain.badge.service;

import com.dailyonepage.backend.domain.badge.dto.*;
import com.dailyonepage.backend.domain.badge.entity.BadgeSet;
import com.dailyonepage.backend.domain.badge.entity.UserBadge;
import com.dailyonepage.backend.domain.badge.entity.UserBadgeSet;
import com.dailyonepage.backend.domain.badge.repository.BadgeSetRepository;
import com.dailyonepage.backend.domain.badge.repository.UserBadgeRepository;
import com.dailyonepage.backend.domain.badge.repository.UserBadgeSetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 뱃지 서비스
 *
 * 뱃지 조회 및 진행 상황 관리
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeSetRepository badgeSetRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserBadgeSetRepository userBadgeSetRepository;

    /**
     * 내 뱃지 현황 조회 (획득 + 진행 중)
     */
    public MyBadgesResponse getMyBadges(Long userId) {
        // 획득한 뱃지 목록
        List<UserBadge> acquiredBadges = userBadgeRepository.findByUserIdWithBadgeInfo(userId);
        List<UserBadgeResponse> acquired = acquiredBadges.stream()
                .map(UserBadgeResponse::from)
                .toList();

        // 진행 중인 뱃지세트 목록
        List<UserBadgeSet> progressBadgeSets = userBadgeSetRepository.findByUserIdWithBadgeInfo(userId);
        List<UserBadgeSetResponse> inProgress = progressBadgeSets.stream()
                .map(UserBadgeSetResponse::from)
                .toList();

        return MyBadgesResponse.of(acquired, inProgress);
    }

    /**
     * 시스템 뱃지세트 목록 조회
     */
    public List<BadgeSetResponse> getSystemBadgeSets() {
        // 범용 뱃지세트 조회
        List<BadgeSet> universalSets = badgeSetRepository.findUniversalBadgeSets();

        return universalSets.stream()
                .map(BadgeSetResponse::from)
                .toList();
    }

    /**
     * 특정 습관에 적용 가능한 뱃지세트 목록 조회
     */
    public List<BadgeSetResponse> getBadgeSetsForHabit(Long habitId) {
        // 해당 습관 전용 + 범용 뱃지세트
        List<BadgeSet> badgeSets = badgeSetRepository.findApplicableBadgeSets(habitId);

        return badgeSets.stream()
                .map(BadgeSetResponse::from)
                .toList();
    }

    /**
     * 최근 획득 뱃지 조회
     */
    public List<UserBadgeResponse> getRecentBadges(Long userId, int limit) {
        List<UserBadge> recentBadges = userBadgeRepository.findRecentBadges(userId, limit);

        return recentBadges.stream()
                .map(UserBadgeResponse::from)
                .toList();
    }
}
