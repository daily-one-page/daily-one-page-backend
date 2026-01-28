package com.dailyonepage.backend.global.exception;

import com.dailyonepage.backend.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리기
 *
 * @RestControllerAdvice: 모든 Controller에서 발생하는 예외를 잡아서 처리
 * 예외별로 적절한 HTTP 상태 코드와 에러 응답을 반환
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * BusinessException 처리
     * Service 계층에서 발생시킨 비즈니스 예외
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), e.getMessage()));
    }

    /**
     * @Valid 유효성 검증 실패 처리
     * DTO에서 @NotBlank, @Email 등 검증 실패 시
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getMessage());
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("유효성 검증에 실패했습니다.");

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE.getCode(), message));
    }

    /**
     * @ModelAttribute 바인딩 실패 처리
     */
    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ApiResponse<Void>> handleBindException(BindException e) {
        log.warn("Binding failed: {}", e.getMessage());
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("요청 데이터 바인딩에 실패했습니다.");

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE.getCode(), message));
    }

    /**
     * 그 외 모든 예외 처리
     * 예상치 못한 서버 오류
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected error occurred", e);
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.error(
                        ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                        ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
