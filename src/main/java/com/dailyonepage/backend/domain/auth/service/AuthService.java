package com.dailyonepage.backend.domain.auth.service;

import com.dailyonepage.backend.domain.auth.dto.LoginRequest;
import com.dailyonepage.backend.domain.auth.dto.SignupRequest;
import com.dailyonepage.backend.domain.auth.dto.TokenReissueRequest;
import com.dailyonepage.backend.domain.auth.dto.TokenResponse;
import com.dailyonepage.backend.domain.user.entity.User;
import com.dailyonepage.backend.domain.user.repository.UserRepository;
import com.dailyonepage.backend.global.exception.BusinessException;
import com.dailyonepage.backend.global.exception.ErrorCode;
import com.dailyonepage.backend.global.security.jwt.JwtProperties;
import com.dailyonepage.backend.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 서비스
 * 회원가입, 로그인, 토큰 재발급 로직 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    /**
     * 회원가입
     */
    @Transactional
    public Long signup(SignupRequest request) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 비밀번호 암호화 후 저장
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .build();

        User savedUser = userRepository.save(user);
        log.info("회원가입 완료: {}", savedUser.getEmail());

        return savedUser.getId();
    }

    /**
     * 로그인
     */
    public TokenResponse login(LoginRequest request) {
        // 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

        log.info("로그인 성공: {}", user.getEmail());

        return TokenResponse.of(
                accessToken,
                refreshToken,
                jwtProperties.getAccessTokenExpiration()
        );
    }

    /**
     * 토큰 재발급 (Refresh Token으로 새 Access Token 발급)
     */
    public TokenResponse reissue(TokenReissueRequest request) {
        String refreshToken = request.getRefreshToken();

        // Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 토큰에서 이메일 추출
        String email = jwtTokenProvider.getEmailFromToken(refreshToken);

        // 사용자 존재 확인
        if (!userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 새 토큰 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(email);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(email);

        log.info("토큰 재발급 완료: {}", email);

        return TokenResponse.of(
                newAccessToken,
                newRefreshToken,
                jwtProperties.getAccessTokenExpiration()
        );
    }
}
