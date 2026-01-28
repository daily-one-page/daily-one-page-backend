package com.dailyonepage.backend.global.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 설정값을 application.yml에서 주입받는 클래스
 *
 * @ConfigurationProperties: prefix가 "jwt"인 설정값들을 자동 바인딩
 * application.yml의 jwt.secret, jwt.access-token-expiration 등을 매핑
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private long accessTokenExpiration;   // ms 단위
    private long refreshTokenExpiration;  // ms 단위
}
