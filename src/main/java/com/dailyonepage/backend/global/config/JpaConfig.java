package com.dailyonepage.backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 활성화 설정
 *
 * @EnableJpaAuditing: @CreatedDate, @LastModifiedDate 어노테이션이 동작하도록 함
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
