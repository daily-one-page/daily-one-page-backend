package com.dailyonepage.backend.domain.dailypage.controller;

import com.dailyonepage.backend.domain.dailypage.dto.*;
import com.dailyonepage.backend.domain.dailypage.service.DailyPageService;
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
 * 데일리 페이지 API Controller
 *
 * 하루 한 장 CRUD API
 */
@Tag(name = "DailyPage", description = "데일리 페이지 API")
@RestController
@RequestMapping("/api/daily-pages")
@RequiredArgsConstructor
public class DailyPageController {

    private final DailyPageService dailyPageService;
    private final UserRepository userRepository;

    /**
     * 페이지 작성
     * POST /api/daily-pages
     */
    @Operation(summary = "페이지 작성", description = "오늘(또는 지정일) 페이지를 작성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<DailyPageResponse>> createPage(
            @Valid @RequestBody DailyPageCreateRequest request) {

        Long userId = getCurrentUserId();
        DailyPageResponse response = dailyPageService.createPage(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 특정 날짜 페이지 조회
     * GET /api/daily-pages?date=2025-01-29
     */
    @Operation(summary = "날짜별 페이지 조회", description = "특정 날짜의 페이지를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<DailyPageResponse>> getPageByDate(
            @Parameter(description = "조회 날짜 (기본값: 오늘)", example = "2025-01-29")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        Long userId = getCurrentUserId();
        LocalDate targetDate = date != null ? date : LocalDate.now();

        DailyPageResponse response = dailyPageService.getPageByDate(userId, targetDate);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 월별 캘린더 조회
     * GET /api/daily-pages/calendar?year=2025&month=1
     */
    @Operation(summary = "월별 캘린더 조회", description = "해당 월에 작성된 페이지 목록을 조회합니다.")
    @GetMapping("/calendar")
    public ResponseEntity<ApiResponse<CalendarResponse>> getCalendar(
            @Parameter(description = "연도", example = "2025")
            @RequestParam int year,
            @Parameter(description = "월", example = "1")
            @RequestParam int month) {

        Long userId = getCurrentUserId();
        CalendarResponse response = dailyPageService.getCalendar(userId, year, month);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 페이지 수정
     * PUT /api/daily-pages/{id}
     */
    @Operation(summary = "페이지 수정", description = "페이지 내용을 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DailyPageResponse>> updatePage(
            @Parameter(description = "페이지 ID", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody DailyPageUpdateRequest request) {

        Long userId = getCurrentUserId();
        DailyPageResponse response = dailyPageService.updatePage(userId, id, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 페이지 삭제
     * DELETE /api/daily-pages/{id}
     */
    @Operation(summary = "페이지 삭제", description = "페이지를 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePage(
            @Parameter(description = "페이지 ID", example = "1")
            @PathVariable Long id) {

        Long userId = getCurrentUserId();
        dailyPageService.deletePage(userId, id);

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
