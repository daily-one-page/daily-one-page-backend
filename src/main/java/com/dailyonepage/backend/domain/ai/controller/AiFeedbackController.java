package com.dailyonepage.backend.domain.ai.controller;

import com.dailyonepage.backend.domain.ai.dto.AiFeedbackHistoryResponse;
import com.dailyonepage.backend.domain.ai.dto.AiFeedbackResponse;
import com.dailyonepage.backend.domain.ai.service.AiFeedbackService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * AI 피드백 API Controller
 *
 * AI 피드백 조회 API
 */
@Tag(name = "AiFeedback", description = "AI 피드백 API")
@RestController
@RequestMapping("/api/ai-feedback")
@RequiredArgsConstructor
public class AiFeedbackController {

    private final AiFeedbackService aiFeedbackService;
    private final UserRepository userRepository;

    /**
     * 오늘의 AI 피드백 조회 (없으면 자동 생성)
     * GET /api/ai-feedback/today
     */
    @Operation(summary = "오늘의 피드백", description = "오늘의 AI 피드백을 조회합니다. 없으면 어제 데이터 기반으로 자동 생성됩니다.")
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<AiFeedbackResponse>> getTodayFeedback() {
        Long userId = getCurrentUserId();
        AiFeedbackResponse response = aiFeedbackService.getTodayFeedback(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 특정 날짜 피드백 조회
     * GET /api/ai-feedback?date=2025-01-29
     */
    @Operation(summary = "날짜별 피드백 조회", description = "특정 날짜의 AI 피드백을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<AiFeedbackResponse>> getFeedbackByDate(
            @Parameter(description = "조회 날짜", example = "2025-01-29")
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        Long userId = getCurrentUserId();
        AiFeedbackResponse response = aiFeedbackService.getFeedbackByDate(userId, date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 월별 피드백 히스토리 조회
     * GET /api/ai-feedback/history?year=2025&month=1
     */
    @Operation(summary = "월별 피드백 히스토리", description = "해당 월의 AI 피드백 목록을 조회합니다.")
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<AiFeedbackHistoryResponse>> getFeedbackHistory(
            @Parameter(description = "연도", example = "2025")
            @RequestParam int year,
            @Parameter(description = "월", example = "1")
            @RequestParam int month) {

        Long userId = getCurrentUserId();
        AiFeedbackHistoryResponse response = aiFeedbackService.getFeedbackHistory(userId, year, month);
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
