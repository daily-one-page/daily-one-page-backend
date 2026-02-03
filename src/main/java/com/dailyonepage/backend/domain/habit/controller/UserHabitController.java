package com.dailyonepage.backend.domain.habit.controller;

import com.dailyonepage.backend.domain.habit.dto.UserHabitCreateRequest;
import com.dailyonepage.backend.domain.habit.dto.UserHabitDetailResponse;
import com.dailyonepage.backend.domain.habit.dto.UserHabitListResponse;
import com.dailyonepage.backend.domain.habit.dto.UserHabitResponse;
import com.dailyonepage.backend.domain.habit.service.UserHabitService;
import com.dailyonepage.backend.domain.user.entity.User;
import com.dailyonepage.backend.domain.user.repository.UserRepository;
import com.dailyonepage.backend.global.common.ApiResponse;
import com.dailyonepage.backend.global.exception.BusinessException;
import com.dailyonepage.backend.global.exception.ErrorCode;
import com.dailyonepage.backend.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 습관 API Controller
 *
 * 사용자가 습관을 등록/해제하는 API
 */
@Tag(name = "UserHabit", description = "사용자 습관 등록/관리 API")
@RestController
@RequestMapping("/api/user-habits")
@RequiredArgsConstructor
public class UserHabitController {

    private final UserHabitService userHabitService;
    private final UserRepository userRepository;

    /**
     * 내 습관 목록 조회
     * GET /api/user-habits
     */
    @Operation(summary = "내 습관 목록 조회", description = "현재 로그인한 사용자가 등록한 습관 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<UserHabitListResponse>> getMyHabits() {
        Long userId = getCurrentUserId();
        UserHabitListResponse response = userHabitService.getMyHabits(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 내 습관 상세 조회
     * GET /api/user-habits/{id}
     */
    @Operation(summary = "내 습관 상세 조회", description = "등록된 습관의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserHabitDetailResponse>> getMyHabitDetail(
            @Parameter(description = "사용자 습관 ID", example = "1")
            @PathVariable Long id) {

        Long userId = getCurrentUserId();
        UserHabitDetailResponse response = userHabitService.getMyHabitDetail(userId, id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 습관 등록
     * POST /api/user-habits
     */
    @Operation(summary = "습관 등록", description = "시스템 습관 또는 본인의 커스텀 습관을 등록합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<UserHabitResponse>> registerHabit(
            @Valid @RequestBody UserHabitCreateRequest request) {

        Long userId = getCurrentUserId();
        UserHabitResponse response = userHabitService.registerHabit(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 습관 해제
     * DELETE /api/user-habits/{id}
     */
    @Operation(summary = "습관 해제", description = "등록된 습관을 해제합니다. (관련 기록도 삭제됨)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> unregisterHabit(
            @Parameter(description = "사용자 습관 ID", example = "1")
            @PathVariable Long id) {

        Long userId = getCurrentUserId();
        userHabitService.unregisterHabit(userId, id);

        return ResponseEntity.noContent().build();
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
