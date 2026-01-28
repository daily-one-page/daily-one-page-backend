package com.dailyonepage.backend.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 토큰 응답 DTO
 */
@Getter
@Builder
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;  // Access Token 만료 시간 (초)

    public static TokenResponse of(String accessToken, String refreshToken, long expiresInMs) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresInMs / 1000)  // ms → 초
                .build();
    }
}
