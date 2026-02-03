package com.dailyonepage.backend.domain.badge.controller;

import com.dailyonepage.backend.domain.badge.dto.BadgeSetResponse;
import com.dailyonepage.backend.domain.badge.dto.MyBadgesResponse;
import com.dailyonepage.backend.domain.badge.dto.UserBadgeResponse;
import com.dailyonepage.backend.domain.badge.service.BadgeService;
import com.dailyonepage.backend.domain.user.entity.User;
import com.dailyonepage.backend.domain.user.repository.UserRepository;
import com.dailyonepage.backend.global.common.ApiResponse;
import com.dailyonepage.backend.global.exception.BusinessException;
import com.dailyonepage.backend.global.exception.ErrorCode;
import com.dailyonepage.backend.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 뱃지 API Controller
 *
 * 뱃지 조회 및 진행 상황 API
 */
@Tag(name = "Badge", description = "뱃지 API")
@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;
    private final UserRepository userRepository;

    /**
     * 전체 뱃지세트 조회
     * GET /api/badges
     */
    @Operation(summary = "전체 뱃지세트 목록", description = "모든 뱃지세트 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BadgeSetResponse>>> getAllBadgeSets() {
        List<BadgeSetResponse> response = badgeService.getAllBadgeSets();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 내 뱃지 현황 조회
     * GET /api/badges/my
     */
    @Operation(summary = "내 뱃지 현황", description = "획득한 뱃지와 진행 중인 뱃지세트를 조회합니다.")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<MyBadgesResponse>> getMyBadges() {
        Long userId = getCurrentUserId();
        MyBadgesResponse response = badgeService.getMyBadges(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 최근 획득 뱃지 조회
     * GET /api/badges/recent?limit=5
     */
    @Operation(summary = "최근 획득 뱃지", description = "최근에 획득한 뱃지를 조회합니다.")
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<UserBadgeResponse>>> getRecentBadges(
            @Parameter(description = "조회 개수 (기본값: 5)", example = "5")
            @RequestParam(defaultValue = "5") int limit) {

        Long userId = getCurrentUserId();
        List<UserBadgeResponse> response = badgeService.getRecentBadges(userId, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 시스템 뱃지세트 목록 조회
     * GET /api/badges/sets
     */
    @Operation(summary = "시스템 뱃지세트 목록", description = "범용으로 사용 가능한 뱃지세트 목록을 조회합니다.")
    @GetMapping("/sets")
    public ResponseEntity<ApiResponse<List<BadgeSetResponse>>> getSystemBadgeSets() {
        List<BadgeSetResponse> response = badgeService.getSystemBadgeSets();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 습관별 적용 가능 뱃지세트 조회
     * GET /api/badges/sets/habit/{habitId}
     */
    @Operation(summary = "습관별 뱃지세트", description = "특정 습관에 적용 가능한 뱃지세트 목록을 조회합니다.")
    @GetMapping("/sets/habit/{habitId}")
    public ResponseEntity<ApiResponse<List<BadgeSetResponse>>> getBadgeSetsForHabit(
            @Parameter(description = "습관 ID", example = "1")
            @PathVariable Long habitId) {

        List<BadgeSetResponse> response = badgeService.getBadgeSetsForHabit(habitId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 현재 로그인한 사용자 ID 조회
     */
    private Long getCurrentUserId() {
        String email = SecurityUtil.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return user.getId();
    }
}
