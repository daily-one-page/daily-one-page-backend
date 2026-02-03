package com.dailyonepage.backend.domain.habit.controller;

import com.dailyonepage.backend.domain.habit.dto.*;
import com.dailyonepage.backend.domain.habit.service.HabitService;
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
 * 습관 API Controller
 *
 * 시스템 습관 조회 및 커스텀 습관 CRUD
 */
@Tag(name = "Habit", description = "습관 관리 API")
@RestController
@RequestMapping("/api/habits")
@RequiredArgsConstructor
public class HabitController {

    private final HabitService habitService;
    private final UserRepository userRepository;

    /**
     * 습관 목록 조회
     * GET /api/habits
     * GET /api/habits?type=system (시스템 습관만)
     * GET /api/habits?type=custom (내 커스텀 습관만)
     */
    @Operation(summary = "습관 목록 조회", description = "습관 목록을 조회합니다. type 미지정시 시스템 습관 반환")
    @GetMapping
    public ResponseEntity<ApiResponse<SystemHabitListResponse>> getHabits(
            @Parameter(description = "습관 타입 (system/custom, 기본값: system)", example = "system")
            @RequestParam(required = false, defaultValue = "system") String type) {

        if ("custom".equals(type)) {
            Long userId = getCurrentUserId();
            SystemHabitListResponse response = habitService.getCustomHabits(userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        }

        // 기본값: system
        SystemHabitListResponse response = habitService.getSystemHabits();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 커스텀 습관 생성
     * POST /api/habits
     */
    @Operation(summary = "커스텀 습관 생성", description = "사용자 정의 습관을 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<HabitResponse>> createHabit(
            @Valid @RequestBody HabitCreateRequest request) {

        Long userId = getCurrentUserId();
        HabitResponse response = habitService.createCustomHabit(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 커스텀 습관 수정
     * PUT /api/habits/{id}
     */
    @Operation(summary = "커스텀 습관 수정", description = "사용자 정의 습관을 수정합니다. (본인 것만 가능)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HabitResponse>> updateHabit(
            @Parameter(description = "습관 ID", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody HabitUpdateRequest request) {

        Long userId = getCurrentUserId();
        HabitResponse response = habitService.updateCustomHabit(userId, id, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 커스텀 습관 삭제
     * DELETE /api/habits/{id}
     */
    @Operation(summary = "커스텀 습관 삭제", description = "사용자 정의 습관을 삭제합니다. (본인 것만 가능, 연관 데이터 Cascade 삭제)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHabit(
            @Parameter(description = "습관 ID", example = "1")
            @PathVariable Long id) {

        Long userId = getCurrentUserId();
        habitService.deleteCustomHabit(userId, id);

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
