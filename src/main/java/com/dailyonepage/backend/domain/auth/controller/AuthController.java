package com.dailyonepage.backend.domain.auth.controller;

import com.dailyonepage.backend.domain.auth.dto.LoginRequest;
import com.dailyonepage.backend.domain.auth.dto.SignupRequest;
import com.dailyonepage.backend.domain.auth.dto.TokenReissueRequest;
import com.dailyonepage.backend.domain.auth.dto.TokenResponse;
import com.dailyonepage.backend.domain.auth.service.AuthService;
import com.dailyonepage.backend.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 API Controller
 */
@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 회원가입
     * POST /api/auth/signup
     */
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Long>> signup(@Valid @RequestBody SignupRequest request) {
        Long userId = authService.signup(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(userId));
    }

    /**
     * 로그인
     * POST /api/auth/login
     */
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 토큰 재발급
     * POST /api/auth/reissue
     */
    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 새 토큰을 발급받습니다.")
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(@Valid @RequestBody TokenReissueRequest request) {
        TokenResponse response = authService.reissue(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
