package com.dailyonepage.backend.global.exception;

import lombok.Getter;

/**
 * 비즈니스 로직 예외
 *
 * Service 계층에서 비즈니스 규칙 위반 시 발생시키는 예외
 * ErrorCode를 포함하여 일관된 에러 응답 제공
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
