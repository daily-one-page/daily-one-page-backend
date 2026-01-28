package com.dailyonepage.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정
 *
 * TODO: JWT 인증 필터 추가 후 permitAll 범위 조정 필요
 * 현재는 개발 편의를 위해 모든 요청 허용
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (REST API는 stateless라 CSRF 불필요)
                .csrf(AbstractHttpConfigurer::disable)
                // 세션 사용 안 함 (JWT 사용)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 요청 인가 설정
                .authorizeHttpRequests(auth -> auth
                        // H2 Console 허용
                        .requestMatchers("/h2-console/**").permitAll()
                        // Swagger UI 허용
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // 인증 관련 API 허용
                        .requestMatchers("/api/auth/**").permitAll()
                        // TODO: 나머지는 인증 필요하도록 변경
                        .anyRequest().permitAll()
                )
                // H2 Console은 iframe 사용하므로 허용
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                // 기본 로그인 폼 비활성화
                .formLogin(AbstractHttpConfigurer::disable)
                // HTTP Basic 인증 비활성화
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * 비밀번호 암호화 인코더
     * BCrypt: 단방향 해시 + 솔트(salt) 자동 적용
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
