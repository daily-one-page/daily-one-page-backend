package com.dailyonepage.backend.domain.habit.controller;

import com.dailyonepage.backend.domain.habit.dto.HabitLogCreateRequest;
import com.dailyonepage.backend.domain.habit.dto.HabitLogListResponse;
import com.dailyonepage.backend.domain.habit.dto.HabitLogResponse;
import com.dailyonepage.backend.domain.habit.service.HabitLogService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 습관 체크 기록 API Controller
 *
 * 습관 체크, 조회, 취소 API
 */
@Tag(name = "HabitLog", description = "습관 체크 기록 API")
@RestController
@RequestMapping("/api/habit-logs")
@RequiredArgsConstructor
public class HabitLogController {

    private final HabitLogService habitLogService;
    private final UserRepository userRepository;

    /**
     * 습관 체크
     * POST /api/habit-logs
     */
    @Operation(summary = "습관 체크", description = "오늘(또는 지정 날짜) 습관을 체크합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<HabitLogResponse>> checkHabit(
            @Valid @RequestBody HabitLogCreateRequest request) {

        Long userId = getCurrentUserId();
        HabitLogResponse response = habitLogService.checkHabit(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 특정 날짜 습관 기록 조회
     * GET /api/habit-logs?date=2025-01-29
     */
    @Operation(summary = "날짜별 습관 기록 조회", description = "특정 날짜의 습관 체크 기록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<HabitLogListResponse>> getLogsByDate(
            @Parameter(description = "조회 날짜 (기본값: 오늘)", example = "2025-01-29")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        Long userId = getCurrentUserId();
        LocalDate targetDate = date != null ? date : LocalDate.now();

        HabitLogListResponse response = habitLogService.getLogsByDate(userId, targetDate);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 습관 체크 취소
     * DELETE /api/habit-logs/{id}
     */
    @Operation(summary = "습관 체크 취소", description = "습관 체크 기록을 삭제합니다. (스트릭 재계산됨)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelCheck(
            @Parameter(description = "로그 ID", example = "1")
            @PathVariable Long id) {

        Long userId = getCurrentUserId();
        habitLogService.cancelCheck(userId, id);

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
