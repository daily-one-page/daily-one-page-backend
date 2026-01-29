package com.dailyonepage.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 정의
 *
 * 코드 네이밍 규칙:
 * - AUTH_xxx: 인증 관련
 * - USER_xxx: 사용자 관련
 * - HABIT_xxx: 습관 관련
 * - BADGE_xxx: 뱃지 관련
 * - PAGE_xxx: 데일리 페이지 관련
 * - COMMON_xxx: 공통
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_002", "서버 오류가 발생했습니다."),

    // Auth
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_001", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "만료된 토큰입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_003", "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_004", "접근 권한이 없습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER_002", "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "USER_003", "비밀번호가 일치하지 않습니다."),

    // Habit
    HABIT_NOT_FOUND(HttpStatus.NOT_FOUND, "HABIT_001", "습관을 찾을 수 없습니다."),
    USER_HABIT_NOT_FOUND(HttpStatus.NOT_FOUND, "HABIT_002", "등록된 습관을 찾을 수 없습니다."),
    DUPLICATE_USER_HABIT(HttpStatus.CONFLICT, "HABIT_003", "이미 등록된 습관입니다."),
    HABIT_NOT_OWNED(HttpStatus.FORBIDDEN, "HABIT_004", "해당 습관에 대한 권한이 없습니다."),
    SYSTEM_HABIT_NOT_MODIFIABLE(HttpStatus.BAD_REQUEST, "HABIT_005", "시스템 습관은 수정/삭제할 수 없습니다."),
    HABIT_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "HABIT_006", "습관 체크 기록을 찾을 수 없습니다."),
    DUPLICATE_HABIT_LOG(HttpStatus.CONFLICT, "HABIT_007", "해당 날짜에 이미 체크 기록이 있습니다."),

    // Badge
    BADGE_SET_NOT_FOUND(HttpStatus.NOT_FOUND, "BADGE_001", "뱃지 세트를 찾을 수 없습니다."),
    BADGE_NOT_FOUND(HttpStatus.NOT_FOUND, "BADGE_002", "뱃지를 찾을 수 없습니다."),

    // DailyPage
    PAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "PAGE_001", "데일리 페이지를 찾을 수 없습니다."),
    DUPLICATE_PAGE(HttpStatus.CONFLICT, "PAGE_002", "해당 날짜에 이미 페이지가 존재합니다."),

    // AI Feedback
    FEEDBACK_NOT_FOUND(HttpStatus.NOT_FOUND, "AI_001", "AI 피드백을 찾을 수 없습니다."),
    NO_DATA_FOR_FEEDBACK(HttpStatus.BAD_REQUEST, "AI_002", "피드백을 생성할 데이터가 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
