package com.dailyonepage.backend.global.security;

import com.dailyonepage.backend.global.exception.BusinessException;
import com.dailyonepage.backend.global.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Security 관련 유틸리티 클래스
 */
public class SecurityUtil {

    private SecurityUtil() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }

    /**
     * 현재 인증된 사용자의 이메일 조회
     *
     * @return 현재 사용자의 이메일
     * @throws BusinessException 인증 정보가 없는 경우
     */
    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getPrincipal().equals("anonymousUser")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return authentication.getName();
    }
}
