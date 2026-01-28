package com.dailyonepage.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토큰 재발급 요청 DTO
 */
@Getter
@NoArgsConstructor
public class TokenReissueRequest {

    @NotBlank(message = "Refresh Token은 필수입니다.")
    private String refreshToken;
}
