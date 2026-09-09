package com.sleepwell.sleepwell_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * JPA Auditing 설정 클래스
 * 
 * Spring Boot 테스트 베스트 프랙티스에 따라 JPA Auditing 설정을 
 * 메인 애플리케이션 클래스에서 분리하여 웹 계층 테스트(@WebMvcTest)에서
 * JPA 관련 의존성을 제외할 수 있도록 합니다.
 * 
 * @see org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {

    /**
     * JPA Auditing을 위한 현재 사용자 정보 제공
     * Spring Security 컨텍스트에서 현재 로그인한 사용자 정보를 가져옵니다.
     * 인증되지 않은 경우 "system"을 기본값으로 사용합니다.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("system");
            }
            
            // AnonymousUser인 경우
            if (authentication.getPrincipal() instanceof String && 
                "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.of("system");
            }
            
            // UserDetails를 구현한 경우 (일반적인 경우)
            if (authentication.getPrincipal() instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                return Optional.of(userDetails.getUsername());
            }
            
            // 기타 경우 principal을 문자열로 변환
            return Optional.of(authentication.getPrincipal().toString());
        };
    }
} 